package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.MessageFilter

interface OnMessageFilterInterface {
    fun onFilterClick(filter: MessageFilter)
}