package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.QuickResponse

interface OnQuickMessageClickInterface {
    fun onQuickMessageItemClick(quickResponse: QuickResponse)
}