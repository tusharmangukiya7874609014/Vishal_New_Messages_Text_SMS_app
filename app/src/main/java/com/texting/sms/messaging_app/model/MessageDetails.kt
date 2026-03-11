package com.texting.sms.messaging_app.model

data class MessageDetails(
    val id: Long,
    val address: String?,
    val body: String?,
    val date: Long,
    val type: Int,
    val contactName: String?,
    val photoUri: String?
)
