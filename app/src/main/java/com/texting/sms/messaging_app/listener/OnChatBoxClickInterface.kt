package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ChatBoxStyle

interface OnChatBoxClickInterface {
    fun onSelectChatBoxClick(position: Int, chatBoxStyle: ChatBoxStyle)
}