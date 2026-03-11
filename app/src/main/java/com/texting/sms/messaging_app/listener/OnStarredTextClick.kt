package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.MessageDetails

interface OnStarredTextClick {
    fun onPlainTextMessageClick(messageDetails: MessageDetails, actionOfTask : String)
}