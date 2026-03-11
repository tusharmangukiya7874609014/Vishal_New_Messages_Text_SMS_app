package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ChatUser

interface OnChatUserInterface {
    fun chatUserClick(userChatDetails: ChatUser)
}