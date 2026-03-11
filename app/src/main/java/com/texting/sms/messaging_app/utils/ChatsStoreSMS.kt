package com.texting.sms.messaging_app.utils

import com.texting.sms.messaging_app.model.ChatMessage

object ChatsStoreSMS {
    val messageMap = mutableMapOf<Long, MutableList<ChatMessage>>()
}