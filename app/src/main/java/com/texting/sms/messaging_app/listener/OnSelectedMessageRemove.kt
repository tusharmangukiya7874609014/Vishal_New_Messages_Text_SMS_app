package com.texting.sms.messaging_app.listener

interface OnSelectedMessageRemove {
    fun onRemoveSelectedMessage(message: String, position: Int)
}