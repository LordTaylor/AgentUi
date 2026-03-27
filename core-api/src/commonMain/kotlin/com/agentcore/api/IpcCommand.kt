// Sealed class IpcCommand and all its subclasses (client → backend commands).
// Each subclass maps to a "cmd" discriminator value per the IPC protocol v1.6.
// Payload data classes for commands live in IpcCommandPayloads.kt.
package com.agentcore.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("cmd")
sealed class IpcCommand {
    @Serializable @SerialName("send_message")
    data class SendMessage(val payload: SendMessagePayload) : IpcCommand()

    @Serializable @SerialName("list_sessions")
    class ListSessions : IpcCommand()

    @Serializable @SerialName("list_tools")
    class ListTools : IpcCommand()

    @Serializable @SerialName("get_session")
    data class GetSession(val payload: GetSessionPayload) : IpcCommand()

    @Serializable @SerialName("set_backend")
    data class SetBackend(val payload: SetBackendPayload) : IpcCommand()

    @Serializable @SerialName("approval_response")
    data class ApprovalResponse(val payload: ApprovalResponsePayload) : IpcCommand()

    @Serializable @SerialName("set_role")
    data class SetRole(val payload: SetRolePayload) : IpcCommand()

    @Serializable @SerialName("get_stats")
    class GetStats : IpcCommand()

    @Serializable @SerialName("get_scratchpad")
    class GetScratchpad : IpcCommand()

    @Serializable @SerialName("update_scratchpad")
    data class UpdateScratchpad(val payload: UpdateScratchpadPayload) : IpcCommand()

    @Serializable @SerialName("start_indexing")
    class StartIndexing : IpcCommand()

    @Serializable @SerialName("get_indexing_status")
    class GetIndexingStatus : IpcCommand()

    @Serializable @SerialName("list_plugins")
    class ListPlugins : IpcCommand()

    @Serializable @SerialName("enable_plugin")
    data class EnablePlugin(val pluginId: String) : IpcCommand()

    @Serializable @SerialName("disable_plugin")
    data class DisablePlugin(val pluginId: String) : IpcCommand()

    @Serializable @SerialName("list_workflows")
    class ListWorkflows : IpcCommand()

    @Serializable @SerialName("start_workflow")
    data class StartWorkflow(val workflowId: String) : IpcCommand()

    @Serializable @SerialName("stop_workflow")
    data class StopWorkflow(val workflowId: String) : IpcCommand()

    @Serializable @SerialName("start_voice_session")
    class StartVoiceSession : IpcCommand()

    @Serializable @SerialName("stop_voice_session")
    class StopVoiceSession : IpcCommand()

    @Serializable @SerialName("speak_text")
    data class SpeakText(val text: String) : IpcCommand()

    @Serializable @SerialName("get_canvas_state")
    class GetCanvasState : IpcCommand()

    @Serializable @SerialName("update_canvas")
    data class UpdateCanvas(val elements: List<CanvasElement>) : IpcCommand()

    @Serializable @SerialName("get_agent_groups")
    class GetAgentGroups : IpcCommand()

    @Serializable @SerialName("assign_task")
    data class AssignTask(val agentId: String, val task: String) : IpcCommand()

    @Serializable @SerialName("get_context_suggestions")
    class GetContextSuggestions : IpcCommand()

    @Serializable @SerialName("cancel")
    data class Cancel(val payload: CancelPayload) : IpcCommand()

    @Serializable @SerialName("ping")
    class Ping : IpcCommand()

    @Serializable @SerialName("list_backends")
    class ListBackends : IpcCommand()

    @Serializable @SerialName("delete_session")
    data class DeleteSession(val payload: DeleteSessionPayload) : IpcCommand()

    @Serializable @SerialName("prune_session")
    data class PruneSession(val payload: PruneSessionPayload) : IpcCommand()

    @Serializable @SerialName("fork_session")
    data class ForkSession(val payload: ForkSessionPayload) : IpcCommand()

    @Serializable @SerialName("get_config")
    class GetConfig : IpcCommand()

    @Serializable @SerialName("reload_tools")
    class ReloadTools : IpcCommand()

    @Serializable @SerialName("set_system_prompt")
    data class SetSystemPrompt(val payload: SetSystemPromptPayload) : IpcCommand()

    @Serializable @SerialName("rename_session")
    data class RenameSession(val payload: RenameSessionPayload) : IpcCommand()

    @Serializable @SerialName("tag_session")
    data class TagSession(val payload: TagSessionPayload) : IpcCommand()

    @Serializable @SerialName("list_sessions_by_tag")
    data class ListSessionsByTag(val payload: ListSessionsByTagPayload) : IpcCommand()

    @Serializable @SerialName("get_tool")
    data class GetTool(val payload: GetToolPayload) : IpcCommand()

    @Serializable @SerialName("enable_tool")
    data class EnableTool(val payload: ToolTogglePayload) : IpcCommand()

    @Serializable @SerialName("disable_tool")
    data class DisableTool(val payload: ToolTogglePayload) : IpcCommand()

    @Serializable @SerialName("update_config")
    data class UpdateConfig(val payload: UpdateConfigPayload) : IpcCommand()

    @Serializable @SerialName("summarize_context")
    data class SummarizeContext(val payload: SummarizeContextPayload) : IpcCommand()

    @Serializable @SerialName("schedule_task")
    data class ScheduleTask(val payload: ScheduleTaskPayload) : IpcCommand()

    @Serializable @SerialName("cancel_scheduled_task")
    data class CancelScheduledTask(val payload: CancelScheduledTaskPayload) : IpcCommand()

    @Serializable @SerialName("list_scheduled_tasks")
    class ListScheduledTasks : IpcCommand()

    @Serializable @SerialName("ping_backend")
    data class PingBackend(val payload: PingBackendPayload) : IpcCommand()

    @Serializable @SerialName("export_session")
    data class ExportSession(val payload: ExportSessionPayload) : IpcCommand()

    @Serializable @SerialName("import_session")
    data class ImportSession(val payload: ImportSessionPayload) : IpcCommand()

    @Serializable @SerialName("test_tool")
    data class TestTool(val payload: TestToolPayload) : IpcCommand()

    @Serializable @SerialName("spawn_subagent")
    data class SpawnSubAgent(val payload: SpawnSubAgentPayload) : IpcCommand()

    @Serializable @SerialName("wait_subagent")
    data class WaitSubAgent(val payload: WaitSubAgentPayload) : IpcCommand()

    @Serializable @SerialName("cancel_subagent")
    data class CancelSubAgent(val payload: CancelSubAgentPayload) : IpcCommand()

    @Serializable @SerialName("list_subagents")
    class ListSubAgents : IpcCommand()

    @Serializable @SerialName("list_checkpoints")
    data class ListCheckpoints(val payload: ListCheckpointsPayload) : IpcCommand()

    @Serializable @SerialName("restore_checkpoint")
    data class RestoreCheckpoint(val payload: RestoreCheckpointPayload) : IpcCommand()

    @Serializable @SerialName("list_models")
    data class ListModels(val payload: ListModelsPayload) : IpcCommand()

    @Serializable @SerialName("update_memory")
    data class UpdateMemory(val payload: UpdateMemoryPayload) : IpcCommand()

    @Serializable @SerialName("restart_provider")
    data class RestartProvider(val payload: RestartProviderPayload) : IpcCommand()

    @Serializable @SerialName("create_tool")
    data class CreateTool(val payload: CreateToolPayload) : IpcCommand()

    @Serializable @SerialName("delete_tool")
    data class DeleteTool(val payload: DeleteToolPayload) : IpcCommand()

    @Serializable @SerialName("approve_plan")
    data class ApprovePlan(val payload: ApprovePlanPayload) : IpcCommand()

    // B2 fix: N11 ask_human response — without this the agent blocks forever.
    @Serializable @SerialName("human_input_response")
    data class HumanInputResponse(val payload: HumanInputResponsePayload) : IpcCommand()

    @Serializable @SerialName("list_skills")
    class ListSkills : IpcCommand()

    // A10: Run a multi-step AgentGroup workflow.
    @Serializable @SerialName("run_workflow")
    data class RunWorkflow(val payload: RunWorkflowPayload) : IpcCommand()

    // A12: List all remembered facts for a session.
    @Serializable @SerialName("list_memory")
    data class ListMemory(val payload: ListMemoryPayload) : IpcCommand()

    // A12: Delete a single fact from a session's memory.
    @Serializable @SerialName("delete_memory")
    data class DeleteMemory(val payload: DeleteMemoryPayload) : IpcCommand()
}
