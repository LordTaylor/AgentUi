// Handles backend/model switching, provider restart, tool management, and workflow intents.
// Also owns the HTTP client used for LM Studio model loading (LmsLoadModel).
// See: docs/COMMUNICATION.md §set_backend, §list_models, §restart_provider for protocol.
package com.agentcore.ui.chat

import androidx.compose.runtime.MutableState
import com.agentcore.api.*
import com.agentcore.shared.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ProviderViewModel(
    private val uiState: MutableState<ChatUiState>,
    private val client: AgentClient,
    private val stdioExecutor: StdioExecutor,
    private val unixSocketExecutor: UnixSocketExecutor,
    private val settingsManager: SettingsManager,
    private val log: (String, String, String) -> Unit,
) {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
    }

    private fun update(block: ChatUiState.() -> ChatUiState) { uiState.value = uiState.value.block() }
    private val st get() = uiState.value

    fun handle(intent: ChatIntent, scope: CoroutineScope, mode: ConnectionMode) {
        when (intent) {
            is ChatIntent.FetchModels              -> fetchModels(intent, scope, mode)
            is ChatIntent.ActivateProvider         -> activateProvider(intent, scope, mode)
            is ChatIntent.ActivateProviderAndRestart -> activateAndRestart(intent, scope, mode)
            is ChatIntent.SaveProviderConfigs      -> saveProviderConfigs(intent)
            is ChatIntent.SaveNamedProviderConfig  -> saveNamedConfig(intent)
            is ChatIntent.DeleteNamedProviderConfig -> deleteNamedConfig(intent)
            is ChatIntent.LoadNamedProviderConfig  -> loadNamedConfig(intent, scope, mode)
            is ChatIntent.LmsLoadModel             -> lmsLoadModel(intent, scope)
            ChatIntent.RestartAgent                -> {
                log("→", "manual_restart", "User requested restart")
                scope.launch(Dispatchers.IO) { if (mode == ConnectionMode.STDIO) stdioExecutor.restart() }
            }
            is ChatIntent.RestartProvider          -> restartProvider(intent, scope, mode)
            is ChatIntent.UpdateSettings           -> updateSettings(intent, scope, mode)
            ChatIntent.ToggleProviderDialog        -> update { copy(showProviderDialog = !showProviderDialog) }
            ChatIntent.ToggleWorkflowDialog        -> update { copy(showWorkflowDialog = !showWorkflowDialog) }
            ChatIntent.ToggleCreateToolDialog      -> update { copy(showCreateToolDialog = !showCreateToolDialog) }
            is ChatIntent.RunWorkflow              -> runWorkflow(intent, scope, mode)
            ChatIntent.ReloadTools                 -> reloadTools(scope, mode)
            ChatIntent.ReloadSkills                -> scope.launch { sendCmd(IpcCommand.ListSkills(), mode) }
            is ChatIntent.CreateTool               -> createTool(intent, scope, mode)
            is ChatIntent.DeleteTool               -> deleteTool(intent, scope, mode)
            ChatIntent.RefreshStats                -> refreshStats(scope, mode)
            else                                   -> {}
        }
    }

    private fun fetchModels(intent: ChatIntent.FetchModels, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "list_models", intent.backend)
        scope.launch { sendCmd(IpcCommand.ListModels(ListModelsPayload(intent.backend, intent.url)), mode) }
    }

    private fun activateProvider(intent: ChatIntent.ActivateProvider, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "set_backend", "${intent.backend} / ${intent.model}")
        scope.launch {
            sendCmd(IpcCommand.SetBackend(SetBackendPayload(intent.backend, intent.model.ifEmpty { null })), mode)
            update { copy(currentBackend = intent.backend, currentModelName = intent.model) }
        }
    }

    private fun activateAndRestart(intent: ChatIntent.ActivateProviderAndRestart, scope: CoroutineScope, mode: ConnectionMode) {
        val newSettings = st.uiSettings.copy(providerConfigs = intent.updatedConfigs)
        update { copy(uiSettings = newSettings) }
        settingsManager.save(newSettings, UiSettings.serializer())
        log("→", "restart_backend", "${intent.backend} → ${intent.envVars.entries.joinToString(", ") { "${it.key}=${it.value}" }.ifEmpty { "(no env vars)" }}")
        scope.launch(Dispatchers.IO) { if (mode == ConnectionMode.STDIO) stdioExecutor.restart(intent.envVars) }
        scope.launch {
            delay(1500)
            val model = intent.updatedConfigs[intent.backend]?.model ?: ""
            sendCmd(IpcCommand.SetBackend(SetBackendPayload(intent.backend, model.ifEmpty { null })), mode)
            update { copy(currentBackend = intent.backend, currentModelName = model) }
        }
    }

    private fun saveProviderConfigs(intent: ChatIntent.SaveProviderConfigs) {
        val newSettings = st.uiSettings.copy(providerConfigs = intent.configs)
        update { copy(uiSettings = newSettings) }
        settingsManager.save(newSettings, UiSettings.serializer())
    }

    private fun saveNamedConfig(intent: ChatIntent.SaveNamedProviderConfig) {
        val saved = st.uiSettings.savedProviderConfigs.toMutableMap()
        val list = saved[intent.backend]?.toMutableList() ?: mutableListOf()
        list.removeAll { it.name == intent.name }
        list.add(SavedProviderConfig(intent.name, intent.config))
        saved[intent.backend] = list
        val newSettings = st.uiSettings.copy(savedProviderConfigs = saved)
        update { copy(uiSettings = newSettings) }
        settingsManager.save(newSettings, UiSettings.serializer())
    }

    private fun deleteNamedConfig(intent: ChatIntent.DeleteNamedProviderConfig) {
        val saved = st.uiSettings.savedProviderConfigs.toMutableMap()
        val list = saved[intent.backend]?.toMutableList() ?: return
        list.removeAll { it.name == intent.name }
        saved[intent.backend] = list
        val newSettings = st.uiSettings.copy(savedProviderConfigs = saved)
        update { copy(uiSettings = newSettings) }
        settingsManager.save(newSettings, UiSettings.serializer())
    }

    private fun loadNamedConfig(intent: ChatIntent.LoadNamedProviderConfig, scope: CoroutineScope, mode: ConnectionMode) {
        val list = st.uiSettings.savedProviderConfigs[intent.backend] ?: return
        val named = list.find { it.name == intent.name } ?: return
        val updated = st.uiSettings.providerConfigs.toMutableMap().also { it[intent.backend] = named.config }
        val newSettings = st.uiSettings.copy(providerConfigs = updated)
        update { copy(uiSettings = newSettings) }
        settingsManager.save(newSettings, UiSettings.serializer())
        if (intent.backend in listOf("lmstudio", "ollama", "huggingface")) {
            fetchModels(ChatIntent.FetchModels(intent.backend, named.config.baseUrl), scope, mode)
        }
    }

    private fun lmsLoadModel(intent: ChatIntent.LmsLoadModel, scope: CoroutineScope) {
        update { copy(loadingModelName = intent.model) }
        scope.launch {
            try {
                log("→", "lms_load_model", intent.model)
                val baseUrl = if (!intent.url.startsWith("http")) "http://${intent.url}" else intent.url
                val response = httpClient.post("${baseUrl}/api/v1/models/load") {
                    contentType(ContentType.Application.Json)
                    if (intent.config.apiKey.isNotEmpty()) header("Authorization", "Bearer ${intent.config.apiKey}")
                    setBody(buildJsonObject {
                        put("model", intent.model); put("echo_load_config", true)
                        intent.config.contextLength?.let { put("context_length", it) }
                        intent.config.evalBatchSize?.let { put("eval_batch_size", it) }
                        intent.config.flashAttention?.let { put("flash_attention", it) }
                        intent.config.numExperts?.let { put("num_experts", it) }
                        intent.config.offloadKvCacheToGpu?.let { put("offload_kv_cache_to_gpu", it) }
                    })
                }
                if (response.status.isSuccess()) {
                    log("←", "lms_load_ok", "Model loaded")
                    update { copy(currentModelName = intent.model) }
                } else {
                    log("←", "lms_load_fail", "HTTP ${response.status.value}")
                }
            } catch (e: Exception) {
                log("←", "lms_load_err", e.message ?: "unknown error")
            } finally {
                update { copy(loadingModelName = null) }
            }
        }
    }

    private fun restartProvider(intent: ChatIntent.RestartProvider, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "restart_provider", intent.provider)
        val model = st.uiSettings.providerConfigs[intent.provider]?.model
        update { copy(loadingModelName = model) }
        scope.launch {
            sendCmd(IpcCommand.RestartProvider(RestartProviderPayload(intent.provider, model)), mode)
            update { copy(loadingModelName = null) }
        }
    }

    private fun updateSettings(intent: ChatIntent.UpdateSettings, scope: CoroutineScope, mode: ConnectionMode) {
        update { copy(currentBackend = intent.backend, currentRole = intent.role, showSettings = false) }
        scope.launch {
            if (mode == ConnectionMode.IPC) {
                client.sendCommand(IpcCommand.SetBackend(SetBackendPayload(intent.backend)))
                client.sendCommand(IpcCommand.SetRole(SetRolePayload(intent.role)))
            }
        }
    }

    private fun runWorkflow(intent: ChatIntent.RunWorkflow, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "run_workflow", "steps=${intent.payload.steps.size}")
        scope.launch { sendCmd(IpcCommand.RunWorkflow(intent.payload), mode) }
    }

    private fun reloadTools(scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            sendCmd(IpcCommand.ReloadTools(), mode)
            if (mode == ConnectionMode.IPC) {
                val tools = client.listTools()
                update { copy(availableTools = tools) }
            }
        }
    }

    private fun createTool(intent: ChatIntent.CreateTool, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "create_tool", intent.name)
        scope.launch {
            if (mode == ConnectionMode.IPC) {
                client.createTool(intent.name, intent.template)
                val tools = client.listTools()
                update { copy(availableTools = tools) }
            }
        }
    }

    private fun deleteTool(intent: ChatIntent.DeleteTool, scope: CoroutineScope, mode: ConnectionMode) {
        log("→", "delete_tool", intent.name)
        scope.launch {
            if (mode == ConnectionMode.IPC) {
                client.deleteTool(intent.name)
                val tools = client.listTools()
                update { copy(availableTools = tools) }
            }
        }
    }

    private fun refreshStats(scope: CoroutineScope, mode: ConnectionMode) {
        scope.launch {
            when (mode) {
                ConnectionMode.IPC   -> { val stats = client.getStats(); update { copy(sessionStats = stats) } }
                ConnectionMode.STDIO -> stdioExecutor.sendCommand(IpcCommand.GetStats())
                else                 -> {}
            }
        }
    }

    private suspend fun sendCmd(cmd: IpcCommand, mode: ConnectionMode) = when (mode) {
        ConnectionMode.IPC         -> client.sendCommand(cmd)
        ConnectionMode.STDIO       -> stdioExecutor.sendCommand(cmd)
        ConnectionMode.UNIX_SOCKET -> unixSocketExecutor.sendCommand(cmd)
        else                       -> {}
    }
}
