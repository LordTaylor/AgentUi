package com.agentcore.ui.components

import androidx.compose.animation.*
import com.agentcore.ui.pickFolderDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.agentcore.api.*
import com.agentcore.api.UiSettings
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@Composable
fun RightSidePanel(
    uiSettings: UiSettings,
    sidePanelWidth: Dp,
    workingDir: String,
    selectedFilePath: String?,
    onFileSelected: (String?) -> Unit,
    availableTools: List<JsonObject>,
    availableSkills: List<SkillInfo>,
    onReloadTools: () -> Unit,
    onReloadSkills: () -> Unit,
    onDeleteTool: (String) -> Unit,
    onCreateTool: () -> Unit,
    sessionStats: JsonObject?,
    logs: List<LogPayload>,
    scratchpadContent: String,
    onScratchpadUpdate: (String) -> Unit,
    terminalTraffic: List<TerminalTrafficPayload>,
    plugins: List<PluginMetadataPayload>,
    workflows: List<WorkflowStatusPayload>,
    canvasElements: List<CanvasElement>,
    agentGroup: AgentGroupPayload?,
    onUpdateUiSettings: (UiSettings) -> Unit,
    onStatsRefresh: () -> Unit,
    onSetWorkingDir: (String) -> Unit = {},
    scope: kotlinx.coroutines.CoroutineScope? = null
) {
    val isAnyPanelVisible = uiSettings.showFiles || uiSettings.showSkills || uiSettings.showStats || 
        uiSettings.showLogs || uiSettings.showScratchpad || uiSettings.showTerminal || 
        uiSettings.showPluginManager || uiSettings.showWorkflowBuilder || uiSettings.showCanvas || 
        uiSettings.showHelp || uiSettings.showOrchestrator

    androidx.compose.animation.AnimatedVisibility(
        visible = isAnyPanelVisible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        Row(modifier = Modifier.fillMaxHeight()) {
            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Column(modifier = Modifier.width(sidePanelWidth).background(MaterialTheme.colorScheme.surface)) {
                // Panel Selector / Working Directory Header
                Row(
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("WORKING DIRECTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = {
                            if (scope != null) {
                                scope.launch(Dispatchers.IO) {
                                    pickFolderDialog(workingDir)?.let { onSetWorkingDir(it) }
                                }
                            }
                        }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.FolderOpen, "Select working directory", modifier = Modifier.size(14.dp), Color.Gray)
                        }
                        IconButton(onClick = onStatsRefresh, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(14.dp), Color.Gray)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // File Tree
                    if (uiSettings.showFiles) {
                        FileTree(
                            rootPath = workingDir.ifEmpty { System.getProperty("user.home") ?: "" },
                            selectedFilePath = selectedFilePath,
                            onFileSelected = onFileSelected
                        )
                    }
                    // Skills Library
                    if (uiSettings.showSkills) {
                        SkillLibrary(
                            tools = availableTools,
                            skills = availableSkills,
                            onReload = {
                                onReloadTools()
                                onReloadSkills()
                            },
                            onCreateTool = onCreateTool,
                            onDeleteTool = onDeleteTool
                        )
                    }
                    
                    // Overlays for other panels
                    if (uiSettings.showStats) StatsPanel(sessionStats) { onUpdateUiSettings(uiSettings.copy(showStats = false)) }
                    if (uiSettings.showLogs) LogsPanel(logs) { onUpdateUiSettings(uiSettings.copy(showLogs = false)) }
                    if (uiSettings.showScratchpad) ScratchpadPanel(scratchpadContent, onScratchpadUpdate, { onScratchpadUpdate(it) }) { onUpdateUiSettings(uiSettings.copy(showScratchpad = false)) }
                    if (uiSettings.showTerminal) TerminalPanel(terminalTraffic) { onUpdateUiSettings(uiSettings.copy(showTerminal = false)) }
                    if (uiSettings.showPluginManager) PluginManagerPanel(plugins) { onUpdateUiSettings(uiSettings.copy(showPluginManager = false)) }
                    if (uiSettings.showWorkflowBuilder) WorkflowBuilderPanel(workflows) { onUpdateUiSettings(uiSettings.copy(showWorkflowBuilder = false)) }
                    if (uiSettings.showCanvas) CanvasPanel(canvasElements) { onUpdateUiSettings(uiSettings.copy(showCanvas = false)) }
                    if (uiSettings.showHelp) HelpPanel { onUpdateUiSettings(uiSettings.copy(showHelp = false)) }
                    if (uiSettings.showOrchestrator) OrchestratorPanel(agentGroup) { onUpdateUiSettings(uiSettings.copy(showOrchestrator = false)) }

                    // File Preview Overlay
                    if (selectedFilePath != null) {
                        FilePreviewPanel(
                            filePath = selectedFilePath,
                            onClose = { onFileSelected(null) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Cauldron Engine Placeholder (Mockup style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Science, null, Modifier.size(32.dp), Color.Gray.copy(alpha = 0.5f))
                        Text("CAULDRON ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray.copy(alpha = 0.5f))
                        Text("Procedural Animation Placeholder", fontSize = 8.sp, color = Color.Gray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

// ── Panel Wrappers ──────────────────────────────────────────────────────────

@Composable
fun StatsPanel(stats: JsonObject?, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        stats?.let { StatsDashboard(it) }
    }
}

@Composable
fun LogsPanel(logs: List<LogPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        LogViewer(logs = logs, onClear = { /* logic */ })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun ScratchpadPanel(content: String, onUpdate: (String) -> Unit, onSaveRequest: (String) -> Unit, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        com.agentcore.ui.components.Scratchpad(content = content, onSave = { onSaveRequest(it) }, onRefresh = { /* logic */ })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun TerminalPanel(traffic: List<TerminalTrafficPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        TerminalViewer(traffic = traffic, onClear = { /* logic */ })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun PluginManagerPanel(plugins: List<PluginMetadataPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        PluginManager(plugins = plugins, onTogglePlugin = { _, _ -> }, onRefresh = { })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun WorkflowBuilderPanel(workflows: List<WorkflowStatusPayload>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        WorkflowBuilder(workflows = workflows, onStartWorkflow = { }, onStopWorkflow = { }, onRefresh = { })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun CanvasPanel(elements: List<CanvasElement>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        InteractiveCanvas(elements = elements, onClear = { }, onRefresh = { })
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Close, null)
        }
    }
}

@Composable
fun HelpPanel(onDismiss: () -> Unit) {
    HelpSystem(onClose = onDismiss, onTryExample = { })
}

@Composable
fun OrchestratorPanel(group: AgentGroupPayload?, onDismiss: () -> Unit) {
    AgentOrchestrator(
        group = group,
        onAssignTask = { _, _ -> },
        onRefresh = { }
    )
}
