package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.QuickResponse

interface OnQuickMessageClickInterface {
    fun onQuickMessageItemClick(quickResponse: QuickResponse)
}