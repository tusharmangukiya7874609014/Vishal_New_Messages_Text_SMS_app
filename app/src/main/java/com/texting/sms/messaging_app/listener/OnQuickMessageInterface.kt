package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.QuickResponse

interface OnQuickMessageInterface {
    fun onQuickMessageDeleteClick(message: QuickResponse, position : Int)

    fun onQuickMessageEditClick(message: QuickResponse, position : Int)
}