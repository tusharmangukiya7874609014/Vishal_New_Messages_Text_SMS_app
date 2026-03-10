package com.chat.sms_text.messages.model

data class MessagesResult(
    val messages: List<ChatModel.MessageItem>,
    val hasMore: Boolean
)
