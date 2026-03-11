package com.texting.sms.messaging_app.model

data class MessagesResult(
    val messages: List<ChatModel.MessageItem>,
    val hasMore: Boolean
)
