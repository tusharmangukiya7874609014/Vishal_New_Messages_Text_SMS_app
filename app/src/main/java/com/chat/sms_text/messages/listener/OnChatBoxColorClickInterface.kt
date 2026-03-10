package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ChatBoxColor

interface OnChatBoxColorClickInterface {
    fun onSelectChatBoxColorClick(position: Int, chatBoxColor: ChatBoxColor)
}