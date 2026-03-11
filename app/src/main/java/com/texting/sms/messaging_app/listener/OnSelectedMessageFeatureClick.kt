package com.texting.sms.messaging_app.listener

interface OnSelectedMessageFeatureClick {
    fun onSelectedMessageClick(linkOrNumber: String, performAction: String)
}