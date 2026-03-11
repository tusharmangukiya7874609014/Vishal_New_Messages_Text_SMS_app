package com.texting.sms.messaging_app.model

data class ChatMessage(
    val message: String,
    val timestamp: Long,
    val isMe: Boolean
)
