package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.Password

interface OnClickKeyboardInterface {
    fun onItemClick(password: Password, position: Int)
}