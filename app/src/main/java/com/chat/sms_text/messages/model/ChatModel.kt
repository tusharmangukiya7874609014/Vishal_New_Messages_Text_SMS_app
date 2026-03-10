package com.chat.sms_text.messages.model

import android.net.Uri

sealed class ChatModel {
    data class Header(val title: Long) : ChatModel()
    data class MessageItem(
        val smsId: Long = 0L,
        var message: String,
        val timestamp: Long,
        val isFromMe: Boolean,
        val isRead: Boolean = true,
        val mediaUri: Uri? = null,
        var isTimeVisible: Boolean = false,
    ) : ChatModel()
}