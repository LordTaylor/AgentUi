package com.agentcore.di

import com.agentcore.api.AgentClient
import com.agentcore.shared.CliExecutor
import com.agentcore.shared.StdioExecutor
import com.agentcore.shared.UnixSocketExecutor
import com.agentcore.ui.connection.ConnectionViewModel
import com.agentcore.ui.chat.ChatViewModel
import org.koin.dsl.module

val appModule = module {
    single { AgentClient() }
    single { CliExecutor() }
    single { StdioExecutor() }
    single { UnixSocketExecutor() }
    
    factory { ConnectionViewModel() }
    factory { ChatViewModel(get(), get(), get(), get()) }
}
