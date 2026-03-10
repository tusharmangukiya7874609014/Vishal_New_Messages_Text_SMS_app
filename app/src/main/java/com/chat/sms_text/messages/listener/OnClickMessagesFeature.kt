package com.chat.sms_text.messages.listener

interface OnClickMessagesFeature {
    fun onClickOfMessageFeature(threadId: Long, type: String)
}