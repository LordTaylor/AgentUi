// Auto-Updater: checks GitHub Releases API on startup and prompts user to download.
// Compares current build version against latest GitHub release tag.
// Shows a non-blocking dialog with release notes and download link.
package com.agentcore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.awt.Desktop
import java.net.URI

private const val GITHUB_REPO = "agentcore-dev/agentcore-ui"
private const val CURRENT_VERSION = "0.9.0"  // Updated per release

data class ReleaseInfo(
    val tag: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val publishedAt: String
)

/** Checks GitHub releases API and returns the latest release if it's newer than current. */
suspend fun checkForUpdate(): ReleaseInfo? = try {
    val client = HttpClient()
    val resp = client.get("https://api.github.com/repos/$GITHUB_REPO/releases/latest") {
        header("Accept", "application/vnd.github.v3+json")
        header("User-Agent", "AgentCore-UI/$CURRENT_VERSION")
    }
    client.close()
    if (resp.status == HttpStatusCode.OK) {
        val json = Json { ignoreUnknownKeys = true }
        val obj = json.decodeFromString<JsonObject>(resp.bodyAsText())
        val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull ?: return null
        // Simple semver comparison: newer if tag > CURRENT_VERSION
        if (isNewerVersion(tag.removePrefix("v"), CURRENT_VERSION)) {
            ReleaseInfo(
                tag = tag,
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: tag,
                body = obj["body"]?.jsonPrimitive?.contentOrNull ?: "",
                htmlUrl = obj["html_url"]?.jsonPrimitive?.contentOrNull ?: "",
                publishedAt = obj["published_at"]?.jsonPrimitive?.contentOrNull ?: ""
            )
        } else null
    } else null
} catch (_: Exception) { null }

private fun isNewerVersion(remote: String, local: String): Boolean {
    fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
    val r = parts(remote); val l = parts(local)
    for (i in 0..maxOf(r.size, l.size) - 1) {
        val rv = r.getOrElse(i) { 0 }; val lv = l.getOrElse(i) { 0 }
        if (rv > lv) return true
        if (rv < lv) return false
    }
    return false
}

/** Dialog shown when a new version is available. */
@Composable
fun UpdateAvailableDialog(release: ReleaseInfo, onDismiss: () -> Unit) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(480.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.SystemUpdate, null, Modifier.size(32.dp), primaryColor)
                    Column {
                        Text("Update Available", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${release.tag} is ready to download", fontSize = 13.sp, color = Color.Gray)
                    }
                }
                HorizontalDivider()
                // Release notes
                Column(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("What's new in ${release.name}:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = release.body.lines().take(20).joinToString("\n"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Version chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VersionChip("Current", "v$CURRENT_VERSION", Color.Gray)
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp), Color.Gray)
                    VersionChip("Latest", release.tag, primaryColor)
                }
                // Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Later")
                    }
                    Button(
                        onClick = {
                            openUrlInBrowser(release.htmlUrl)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download")
                    }
                }
            }
        }
    }
}

@Composable
private fun VersionChip(label: String, version: String, color: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = color.copy(alpha = 0.7f))
            Text(version, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

/** Composable that checks for updates on first launch and shows dialog if available. */
@Composable
fun AutoUpdateChecker(enabled: Boolean = true) {
    if (!enabled) return
    var release by remember { mutableStateOf<ReleaseInfo?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            try { release = checkForUpdate() } catch (_: Exception) {}
        }
    }
    release?.let { r ->
        UpdateAvailableDialog(release = r, onDismiss = { release = null })
    }
}

fun openUrlInBrowser(url: String) {
    try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(url))
        }
    } catch (_: Exception) {}
}
