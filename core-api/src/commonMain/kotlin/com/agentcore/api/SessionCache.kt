package com.agentcore.api

import com.agentcore.model.Message
import kotlinx.serialization.Serializable

@Serializable
data class SessionCache(
    val sessions: List<SessionInfo> = emptyList(),
    val sessionMessages: Map<String, List<Message>> = emptyMap()
)
