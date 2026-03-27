// Hook Management UI: lists Python hook scripts from ~/.agentcore/hooks/.
// Hooks can be enabled/disabled by renaming (adding/removing .disabled suffix).
// Allows viewing hook source code and toggling active state without IPC.
package com.agentcore.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class HookScript(
    val file: File,
    val name: String,
    val isEnabled: Boolean,
    val sizeBytes: Long
)

@Composable
fun HookManagerPanel(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hooksDir = remember { File(System.getProperty("user.home") ?: ".", ".agentcore/hooks") }
    var hooks by remember { mutableStateOf<List<HookScript>>(emptyList()) }
    var selectedHook by remember { mutableStateOf<HookScript?>(null) }

    fun reload() {
        hooks = if (hooksDir.exists()) {
            hooksDir.listFiles { f -> f.extension == "py" || f.name.endsWith(".py.disabled") }
                ?.map { f ->
                    val isEnabled = !f.name.endsWith(".disabled")
                    HookScript(
                        file = f,
                        name = f.name.removeSuffix(".disabled"),
                        isEnabled = isEnabled,
                        sizeBytes = f.length()
                    )
                }
                ?.sortedBy { it.name } ?: emptyList()
        } else emptyList()
    }

    LaunchedEffect(hooksDir) { reload() }

    Column(modifier = modifier.fillMaxSize().padding(12.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HOOK SCRIPTS",
                fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppTooltip("Refresh") {
                    IconButton(onClick = { reload() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp))
                    }
                }
                AppTooltip("Close") {
                    IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        Text(
            hooksDir.absolutePath,
            fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )

        if (hooks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Code, null, modifier = Modifier.size(32.dp), tint = Color.Gray.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("No hook scripts found", fontSize = 12.sp, color = Color.Gray)
                    Text("Add .py files to\n${hooksDir.absolutePath}", fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.6f))
                }
            }
        } else {
            // List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(hooks, key = { it.name }) { hook ->
                    HookRow(
                        hook = hook,
                        isSelected = selectedHook?.name == hook.name,
                        onToggle = { enable ->
                            val newFile = if (enable)
                                File(hooksDir, hook.name)
                            else
                                File(hooksDir, "${hook.name}.disabled")
                            try { hook.file.renameTo(newFile) } catch (_: Exception) {}
                            reload()
                        },
                        onView = { selectedHook = if (selectedHook?.name == hook.name) null else hook }
                    )
                }
            }

            // Code viewer
            selectedHook?.let { hook ->
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                Text(hook.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                val content = remember(hook.file) {
                    runCatching { hook.file.readText() }.getOrElse { "Error reading file: ${it.message}" }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()
                ) {
                    Text(
                        content,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HookRow(
    hook: HookScript,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    onView: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = hook.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.7f).height(20.dp)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    hook.name, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    color = if (hook.isEnabled) MaterialTheme.colorScheme.onSurface
                            else Color.Gray,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text("${hook.sizeBytes}B", fontSize = 9.sp, color = Color.Gray)
            }
            AppTooltip("View source") {
                IconButton(onClick = onView, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isSelected) Icons.Default.ExpandLess else Icons.Default.Code,
                        null, modifier = Modifier.size(14.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

private fun Modifier.scale(scale: Float) = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
        placeable.placeRelative(
            ((placeable.width * (1 - scale)) / 2).toInt(),
            ((placeable.height * (1 - scale)) / 2).toInt()
        )
    }
}
