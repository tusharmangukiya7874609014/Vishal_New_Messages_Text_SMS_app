package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ChatBoxColor

interface OnChatBoxColorClickInterface {
    fun onSelectChatBoxColorClick(position: Int, chatBoxColor: ChatBoxColor)
}