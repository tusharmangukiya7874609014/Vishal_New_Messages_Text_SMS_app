package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.MessageDetails

interface OnStarredTextClick {
    fun onPlainTextMessageClick(messageDetails: MessageDetails, actionOfTask : String)
}