package com.chat.sms_text.messages.model

data class MessageDetails(
    val id: Long,
    val address: String?,
    val body: String?,
    val date: Long,
    val type: Int,
    val contactName: String?,
    val photoUri: String?
)
