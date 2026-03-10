package com.chat.sms_text.messages.model

data class SMSMessage(
    val threadId: Long,
    val address: String,
    val body: String,
    var contactName: String,
    val photoUri : String,
    val timestamp: Long
)