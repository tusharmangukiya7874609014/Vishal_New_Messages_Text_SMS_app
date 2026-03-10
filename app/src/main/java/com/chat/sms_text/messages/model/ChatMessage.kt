package com.chat.sms_text.messages.model

data class ChatMessage(
    val message: String,
    val timestamp: Long,
    val isMe: Boolean
)
