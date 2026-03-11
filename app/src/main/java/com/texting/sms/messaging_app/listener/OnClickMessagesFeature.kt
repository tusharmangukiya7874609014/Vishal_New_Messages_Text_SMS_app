package com.texting.sms.messaging_app.listener

interface OnClickMessagesFeature {
    fun onClickOfMessageFeature(threadId: Long, type: String)
}