package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ChatBoxStyle

interface OnChatBoxClickInterface {
    fun onSelectChatBoxClick(position: Int, chatBoxStyle: ChatBoxStyle)
}