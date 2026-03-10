package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.MessageDetails

interface OnStarredLinkClick {
    fun onLinkMessageClick(messageDetails: MessageDetails, actionOfTask: String)
}