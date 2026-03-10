package com.chat.sms_text.messages.listener

interface OnSelectedMessageFeatureClick {
    fun onSelectedMessageClick(linkOrNumber: String, performAction: String)
}