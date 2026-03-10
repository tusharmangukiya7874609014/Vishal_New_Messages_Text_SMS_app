package com.chat.sms_text.messages.utils

import com.chat.sms_text.messages.model.ChatMessage

object ChatsStoreSMS {
    val messageMap = mutableMapOf<Long, MutableList<ChatMessage>>()
}