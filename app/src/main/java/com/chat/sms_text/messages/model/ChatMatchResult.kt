package com.chat.sms_text.messages.model

data class ChatMatchResult(
    val threadId: Long,
    val address: String,
    var contactName: String,
    var photoUri: String,
    val matchCount: Int
)