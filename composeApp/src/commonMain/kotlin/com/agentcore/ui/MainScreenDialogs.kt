// Overlay dialogs rendered on top of the main layout in MainScreen.
// Gathers DevOptions, AutoAccept, CreateTool, Approval, PlanApproval,
// TokenAnalytics, WorkflowRun, and MemoryPanel into one composable.
// Called by MainScreen after the layout Row.

package com.agentcore.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.agentcore.api.*
import com.agentcore.shared.ConnectionMode
import com.agentcore.ui.chat.ChatIntent
import com.agentcore.ui.components.*
import kotlinx.coroutines.CoroutineScope

@Composable
fun MainScreenDialogs(
    // DevOptions dialog
    showDevOptionsDialog: Boolean,
    devModeOptions: DevModeOptions,
    onDismissDevOptions: () -> Unit,
    onApplyDevOptions: (DevModeOptions) -> Unit,
    // AutoAccept dialog
    showAutoAcceptDialog: Boolean,
    autoAccept: Boolean,
    bypassAllPermissions: Boolean,
    onDismissAutoAccept: () -> Unit,
    onConfirmAutoAccept: (Boolean, Boolean) -> Unit,
    // CreateTool dialog
    showCreateToolDialog: Boolean,
    newToolName: String,
    onNewToolNameChange: (String) -> Unit,
    onConfirmCreateTool: () -> Unit,
    onDismissCreateTool: () -> Unit,
    // Approval dialog
    pendingApproval: ApprovalRequestPayload?,
    onResolveApproval: (Boolean) -> Unit,
    // PlanApproval dialog
    pendingPlan: PlanReadyPayload?,
    onResolvePlan: (Boolean) -> Unit,
    // TokenAnalytics dialog
    showTokenAnalytics: Boolean,
    tokenHistory: List<UsagePayload>,
    onDismissTokenAnalytics: () -> Unit,
    // Workflow dialog
    showWorkflowDialog: Boolean,
    onIntent: (ChatIntent, CoroutineScope, ConnectionMode) -> Unit,
    scope: CoroutineScope,
    mode: ConnectionMode,
    onDismissWorkflow: () -> Unit,
    // Memory panel
    showMemoryPanel: Boolean,
    currentSessionId: String?,
    memoryFacts: Map<String, String>,
    onLoadMemory: (String) -> Unit,
    onDeleteMemoryKey: (String) -> Unit,
    onToggleMemoryPanel: () -> Unit,
    // ToolDetailDialog
    selectedToolDetail: kotlinx.serialization.json.JsonObject? = null,
    onDismissToolDetail: () -> Unit = {},
    // CheckpointRestoreDialog
    showCheckpointDialog: Boolean = false,
    checkpoints: List<Int> = emptyList(),
    checkpointSessionId: String = "",
    onRestoreCheckpoint: (Int) -> Unit = {},
    onDismissCheckpointDialog: () -> Unit = {}
) {
    if (showDevOptionsDialog) {
        ChatDisplaySettingsDialog(
            options = devModeOptions,
            onDismiss = onDismissDevOptions,
            onApply = onApplyDevOptions
        )
    }

    if (showAutoAcceptDialog) {
        AutoAcceptDialog(
            currentAutoAccept = autoAccept,
            currentBypassAll = bypassAllPermissions,
            onDismiss = onDismissAutoAccept,
            onConfirm = { auto, bypass -> onConfirmAutoAccept(auto, bypass) }
        )
    }

    if (showCreateToolDialog) {
        AlertDialog(
            onDismissRequest = onDismissCreateTool,
            title = { Text("Create New Tool") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newToolName,
                        onValueChange = onNewToolNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tool Name") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = onConfirmCreateTool) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCreateTool) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingApproval != null) {
        ApprovalDialog(
            request = pendingApproval,
            onRespond = onResolveApproval
        )
    }

    if (pendingPlan != null) {
        PlanApprovalDialog(
            plan = pendingPlan,
            onResolve = onResolvePlan
        )
    }

    if (showTokenAnalytics) {
        TokenAnalyticsDialog(
            history = tokenHistory,
            onDismiss = onDismissTokenAnalytics
        )
    }

    if (showWorkflowDialog) {
        WorkflowRunDialog(
            onIntent = onIntent,
            scope = scope,
            mode = mode,
            onDismiss = onDismissWorkflow
        )
    }

    if (selectedToolDetail != null) {
        ToolDetailDialog(
            tool = selectedToolDetail,
            onDismiss = onDismissToolDetail
        )
    }

    if (showCheckpointDialog && checkpoints.isNotEmpty()) {
        CheckpointRestoreDialog(
            sessionId = checkpointSessionId,
            checkpoints = checkpoints,
            onRestore = onRestoreCheckpoint,
            onDismiss = onDismissCheckpointDialog
        )
    }

    if (showMemoryPanel) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onToggleMemoryPanel) {
            MemoryPanel(
                sessionId = currentSessionId,
                facts = memoryFacts,
                onRefresh = {
                    val sid = currentSessionId
                    if (sid != null) onLoadMemory(sid)
                },
                onDeleteKey = onDeleteMemoryKey,
                onClose = onToggleMemoryPanel,
                modifier = Modifier.fillMaxWidth(0.5f)
            )
        }
    }
}
