package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.MessageDetails

interface OnStarredLinkClick {
    fun onLinkMessageClick(messageDetails: MessageDetails, actionOfTask: String)
}