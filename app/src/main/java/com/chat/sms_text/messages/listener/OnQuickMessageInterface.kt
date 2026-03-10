package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.QuickResponse

interface OnQuickMessageInterface {
    fun onQuickMessageDeleteClick(message: QuickResponse, position : Int)

    fun onQuickMessageEditClick(message: QuickResponse, position : Int)
}