package com.agentcore.di

import com.agentcore.api.AgentClient
import com.agentcore.shared.CliExecutor
import com.agentcore.shared.StdioExecutor
import com.agentcore.shared.UnixSocketExecutor
import com.agentcore.shared.SettingsManager
import com.agentcore.ui.connection.ConnectionViewModel
import com.agentcore.ui.chat.ChatViewModel
import org.koin.dsl.module

val appModule = module {
    single { AgentClient() }
    single { CliExecutor() }
    single { StdioExecutor() }
    single { UnixSocketExecutor() }
    
    val home = System.getProperty("user.home") ?: ""
    single { SettingsManager("$home/.agentcore/ui_settings.json") }
    single(qualifier = org.koin.core.qualifier.named("sessionCache")) { SettingsManager("$home/.agentcore/session_cache.json") }
    
    factory { ConnectionViewModel() }
    factory { ChatViewModel(get(), get(), get(), get(), get(), get(org.koin.core.qualifier.named("sessionCache"))) }
}
