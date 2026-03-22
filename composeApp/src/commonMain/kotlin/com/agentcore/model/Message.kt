package com.agentcore.model

enum class MessageType {
    TEXT, ACTION, SYSTEM
}

data class Message(
    val id: String,
    val sender: String,
    val text: String,
    val isFromUser: Boolean,
    val type: MessageType = MessageType.TEXT,
    val attachments: List<String>? = null,
    val extraContent: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
