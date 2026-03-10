package com.chat.sms_text.messages.listener

interface OnSelectedMessageRemove {
    fun onRemoveSelectedMessage(message: String, position: Int)
}