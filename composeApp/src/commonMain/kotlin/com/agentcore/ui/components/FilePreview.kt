package com.agentcore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val TEXT_EXTENSIONS = setOf(
    "txt", "md", "kt", "kts", "java", "py", "rs", "js", "ts", "jsx", "tsx",
    "json", "toml", "yaml", "yml", "xml", "html", "htm", "css", "scss", "sass",
    "sh", "bash", "zsh", "fish", "gradle", "properties", "env", "log",
    "csv", "ini", "cfg", "conf", "sql", "graphql", "proto", "go",
    "c", "cpp", "h", "hpp", "cs", "rb", "php", "swift", "dart", "lock",
    "gitignore", "dockerignore", "editorconfig"
)
private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "bmp", "svg", "webp", "ico", "tiff", "tif")
private const val MAX_FILE_BYTES = 512 * 1024L // 512 KB
private const val MAX_LINES = 3000

@Composable
fun FilePreviewPanel(
    filePath: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val file = remember(filePath) { File(filePath) }
    val ext = remember(filePath) { file.extension.lowercase() }

    val content: String? = remember(filePath) {
        when {
            IMAGE_EXTENSIONS.contains(ext) -> null
            TEXT_EXTENSIONS.contains(ext) || isLikelyText(file) -> readFileContent(file)
            else -> null
        }
    }
    val isImage = IMAGE_EXTENSIONS.contains(ext)

    Column(modifier = modifier.fillMaxHeight().background(MaterialTheme.colorScheme.surface)) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = file.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (file.length() > 0) formatSize(file.length()) else "",
                fontSize = 10.sp,
                color = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp)
            )
            AppTooltip("Zamknij podgląd") {
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

        // ── Content ─────────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                content != null -> {
                    val vScroll = rememberScrollState()
                    val hScroll = rememberScrollState()
                    SelectionContainer {
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(vScroll)
                                .horizontalScroll(hScroll)
                                .padding(10.dp)
                        )
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Brak podglądu",
                                color = Color.Gray.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isImage) "plik graficzny (.${ext})" else "plik binarny (.${ext})",
                                color = Color.Gray.copy(alpha = 0.35f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun readFileContent(file: File): String? {
    return try {
        if (!file.isFile) return null
        if (file.length() > MAX_FILE_BYTES) {
            val lines = file.bufferedReader().useLines { it.take(MAX_LINES).toList() }
            lines.joinToString("\n") + "\n\n… plik za duży — pokazano pierwsze $MAX_LINES linii …"
        } else {
            file.readText()
        }
    } catch (_: Exception) { null }
}

private fun isLikelyText(file: File): Boolean {
    if (!file.isFile || file.length() == 0L) return false
    return try {
        val bytes = file.inputStream().use { stream -> stream.readNBytes(512) }
        bytes.none { it == 0.toByte() }
    } catch (_: Exception) { false }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes} B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
}
