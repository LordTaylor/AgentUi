// Helper functions for exporting chat sessions to Markdown, HTML, and PDF.
// buildMarkdownExport: plain text with code block fencing.
// buildHtmlExport: standalone Catppuccin-Mocha themed HTML file with inline styles.
// buildPdfExport (expect/actual): paginated A4 PDF with Apache PDFBox on JVM.
package com.agentcore.ui.chat

import com.agentcore.model.Message
import com.agentcore.model.MessageType

/** Platform-specific PDF generation. Returns raw PDF bytes, or null on failure. */
internal expect fun buildPdfExport(sid: String, messages: List<Message>): ByteArray?

internal fun buildMarkdownExport(sid: String, messages: List<Message>): String = buildString {
    appendLine("# Chat Session Export")
    appendLine("Session ID: $sid")
    appendLine("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
    appendLine("\n---\n")
    messages.forEach { msg ->
        appendLine("### ${msg.sender}${if (msg.isFromUser) " (User)" else ""}")
        appendLine(msg.text)
        val attachCount = msg.attachments.orEmpty().size
        if (attachCount > 0) appendLine("Attached: $attachCount image(s)")
        appendLine()
    }
}

internal fun buildHtmlExport(sid: String, messages: List<Message>): String {
    val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())
    val rows = messages.filter { it.type != MessageType.ACTION }.joinToString("\n") { msg ->
        val role = if (msg.isFromUser) "user" else "agent"
        val escaped = msg.text
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\n", "<br>")
        """<div class="msg $role"><span class="sender">${msg.sender}</span><div class="body">$escaped</div></div>"""
    }
    return """<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8">
<title>Session $sid</title>
<style>
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background:#1e1e2e; color:#cdd6f4; max-width:860px; margin:40px auto; padding:0 20px; }
h1 { color:#89b4fa; border-bottom:1px solid #313244; padding-bottom:12px; }
.meta { color:#6c7086; font-size:13px; margin-bottom:24px; }
.msg { margin:16px 0; display:flex; flex-direction:column; }
.msg.user { align-items:flex-end; }
.msg.agent { align-items:flex-start; }
.sender { font-size:11px; font-weight:600; color:#6c7086; margin-bottom:4px; }
.body { background:#313244; border-radius:12px; padding:10px 14px; max-width:80%; line-height:1.6; font-size:14px; }
.msg.user .body { background:#45475a; }
pre { background:#181825; padding:10px; border-radius:6px; overflow-x:auto; font-size:12px; }
</style></head><body>
<h1>Chat Session Export</h1>
<p class="meta">Session ID: $sid &nbsp;|&nbsp; Exported: $date</p>
$rows
</body></html>"""
}
