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
import com.agentcore.model.Message
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
    onGetToolDetail: ((String) -> Unit)? = null,
    sessionStats: JsonObject?,
    logs: List<LogPayload>,
    scratchpadContent: String,
    onScratchpadUpdate: (String) -> Unit,
    terminalTraffic: List<TerminalTrafficPayload>,
    plugins: List<PluginMetadataPayload>,
    onToggleTool: (String, Boolean) -> Unit = { _, _ -> },
    workflows: List<WorkflowStatusPayload>,
    canvasElements: List<CanvasElement>,
    agentGroup: AgentGroupPayload?,
    onUpdateUiSettings: (UiSettings) -> Unit,
    onStatsRefresh: () -> Unit,
    onSetWorkingDir: (String) -> Unit = {},
    scope: kotlinx.coroutines.CoroutineScope? = null,
    backendHealth: Map<String, com.agentcore.api.PingResultPayload> = emptyMap(),
    onLoadBackendHealth: () -> Unit = {},
    currentSystemPrompt: String = "",
    onSetSystemPrompt: (String) -> Unit = {},
    messages: List<Message> = emptyList(),
    scheduledTasks: List<ScheduledTaskInfo> = emptyList(),
    onScheduleTask: (String, String?, String?) -> Unit = { _, _, _ -> },
    agentServerUrl: String = "http://localhost:7700"
) {
    val isAnyPanelVisible = uiSettings.showFiles || uiSettings.showSkills || uiSettings.showStats ||
        uiSettings.showLogs || uiSettings.showScratchpad || uiSettings.showTerminal ||
        uiSettings.showPluginManager || uiSettings.showWorkflowBuilder || uiSettings.showCanvas ||
        uiSettings.showHelp || uiSettings.showOrchestrator || uiSettings.showBackendHealth ||
        uiSettings.showArchiveBrowser || uiSettings.showHookManager || uiSettings.showPromptLibrary ||
        uiSettings.showToolEditor || uiSettings.showScheduler || uiSettings.showPinnedContext ||
        uiSettings.showMetrics || uiSettings.showLocalModels || uiSettings.showTimeline ||
        uiSettings.showContextPruning || uiSettings.showPersonalityLab || uiSettings.showHeatmap

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
                    // Only one panel renders at a time — `when` guarantees exclusivity
                    // even if multiple flags are somehow true (e.g. from old persisted settings).
                    val close: (UiSettings) -> Unit = { onUpdateUiSettings(it.withAllPanelsClosed()) }
                    when {
                        uiSettings.showFiles -> FileTree(
                            rootPath = workingDir.ifEmpty { System.getProperty("user.home") ?: "" },
                            selectedFilePath = selectedFilePath,
                            onFileSelected = onFileSelected
                        )
                        uiSettings.showSkills -> SkillLibrary(
                            tools = availableTools,
                            skills = availableSkills,
                            onReload = { onReloadTools(); onReloadSkills() },
                            onCreateTool = onCreateTool,
                            onDeleteTool = onDeleteTool,
                            onToolDetail = if (onGetToolDetail != null) {
                                { tool -> tool["name"]?.jsonPrimitive?.contentOrNull?.let { onGetToolDetail(it) } }
                            } else null
                        )
                        uiSettings.showStats -> StatsPanel(sessionStats) { close(uiSettings) }
                        uiSettings.showLogs -> LogsPanel(logs) { close(uiSettings) }
                        uiSettings.showScratchpad -> ScratchpadPanel(scratchpadContent, onScratchpadUpdate, { onScratchpadUpdate(it) }) { close(uiSettings) }
                        uiSettings.showTerminal -> TerminalPanel(terminalTraffic) { close(uiSettings) }
                        uiSettings.showPluginManager -> PluginManagerPanel(plugins, onToggleTool) { close(uiSettings) }
                        uiSettings.showWorkflowBuilder -> WorkflowBuilderPanel(workflows) { close(uiSettings) }
                        uiSettings.showCanvas -> CanvasPanel(canvasElements) { close(uiSettings) }
                        uiSettings.showHelp -> HelpPanel { close(uiSettings) }
                        uiSettings.showOrchestrator -> OrchestratorPanel(agentGroup) { close(uiSettings) }
                        uiSettings.showBackendHealth -> BackendHealthPanel(
                            backends = emptyList(),
                            health = backendHealth,
                            onPingAll = onLoadBackendHealth,
                            onClose = { close(uiSettings) }
                        )
                        uiSettings.showArchiveBrowser -> SessionArchiveBrowser(onClose = { close(uiSettings) })
                        uiSettings.showHookManager -> HookManagerPanel(onClose = { close(uiSettings) })
                        uiSettings.showPromptLibrary -> PromptLibraryPanel(
                            currentPrompt = currentSystemPrompt,
                            onApply = { prompt -> onSetSystemPrompt(prompt); close(uiSettings) },
                            onClose = { close(uiSettings) }
                        )
                        uiSettings.showToolEditor -> ToolEditorPanel(onClose = { close(uiSettings) })
                        uiSettings.showScheduler -> SchedulerPanel(
                            tasks = scheduledTasks,
                            onSchedule = onScheduleTask,
                            onClose = { close(uiSettings) }
                        )
                        uiSettings.showPinnedContext -> PinnedContextPanel(
                            pinnedFiles = uiSettings.pinnedContextFiles,
                            onAddFile = { f -> onUpdateUiSettings(uiSettings.copy(pinnedContextFiles = uiSettings.pinnedContextFiles + f)) },
                            onRemoveFile = { f -> onUpdateUiSettings(uiSettings.copy(pinnedContextFiles = uiSettings.pinnedContextFiles - f)) },
                            onClose = { close(uiSettings) }
                        )
                        uiSettings.showMetrics -> MetricsPanel(serverUrl = agentServerUrl, onClose = { close(uiSettings) })
                        uiSettings.showLocalModels -> LocalModelManager(onClose = { close(uiSettings) })
                        uiSettings.showTimeline -> SessionTimelinePanel(messages = messages, onClose = { close(uiSettings) })
                        uiSettings.showContextPruning -> SmartContextPruning(messages = messages, onClose = { close(uiSettings) })
                        uiSettings.showPersonalityLab -> PersonalityLab(
                            currentPrompt = currentSystemPrompt,
                            onApplyPersona = { prompt -> onSetSystemPrompt(prompt); close(uiSettings) },
                            onClose = { close(uiSettings) }
                        )
                        uiSettings.showHeatmap -> MultiAgentHeatmap(
                            messages = messages,
                            agentGroup = agentGroup,
                            onClose = { close(uiSettings) }
                        )
                    }

                    // File Preview Overlay — shown on top of the active panel when a file is selected
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

// Panel wrapper composables are in RightSidePanelWrappers.kt
