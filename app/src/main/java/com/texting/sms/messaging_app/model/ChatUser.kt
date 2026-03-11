package com.texting.sms.messaging_app.model

data class ChatUser(
    var threadId: Long,
    val latestMessage: String,
    val timestamp: Long,
    val address: String,
    var contactName: String?,
    var photoUri: String?,
    var unreadCount: Int = 0,
    var isPinned: Boolean = false,
    val simSlot: Int,
    var isMessageSelected: Boolean = false
)