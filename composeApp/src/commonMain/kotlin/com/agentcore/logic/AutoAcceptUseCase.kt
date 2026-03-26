package com.agentcore.logic

import com.agentcore.api.*
import com.agentcore.shared.ConnectionMode
import com.agentcore.shared.StdioExecutor
import com.agentcore.shared.UnixSocketExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * UseCase responsible for synchronizing the Auto-Accept (approval_mode) setting
 * and BypassAll (unsafe mode) between the UI and the backend agent.
 *
 * "Auto-Accept" in UI = "approval_mode: false" + risk-based auto-decision in Backend.
 * "BypassAll" in UI = "bypass_approval: true" in Backend (skips ALL approval logic).
 */
class AutoAcceptUseCase(
    private val client: AgentClient,
    private val stdioExecutor: StdioExecutor,
    private val unixSocketExecutor: UnixSocketExecutor
) {
    /**
     * Synchronizes the auto-accept and bypass setting to the backend.
     *
     * @param autoAccept If true, backend uses risk-based auto-decision (approval_mode=false).
     * @param bypassAll  If true, backend skips ALL approval/permission checks (bypass_approval=true).
     *                   This is the "unsafe mode" — agent acts fully autonomously.
     */
    fun sync(scope: CoroutineScope, mode: ConnectionMode, autoAccept: Boolean, bypassAll: Boolean = false) {
        val isApprovalRequired = !autoAccept && !bypassAll
        val approvalPayload = JsonPrimitive(isApprovalRequired)

        scope.launch {
            val commands = mutableListOf(
                IpcCommand.UpdateConfig(UpdateConfigPayload("approval_mode", approvalPayload)),
                IpcCommand.UpdateConfig(UpdateConfigPayload("plan_before_act", JsonPrimitive(false))),
                IpcCommand.UpdateConfig(UpdateConfigPayload("confidence_gate_approval", JsonPrimitive(false))),
                // WISC: bypass_approval controls the full risk-based auto-decision system
                IpcCommand.UpdateConfig(UpdateConfigPayload("bypass_approval", JsonPrimitive(bypassAll)))
            )

            commands.forEach { cmd ->
                when (mode) {
                    ConnectionMode.STDIO -> stdioExecutor.sendCommand(cmd)
                    ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
                    ConnectionMode.IPC -> client.sendCommand(cmd)
                    else -> {}
                }
            }
        }
    }
}
