package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ChatMatchResult

interface OnSearchResultClickInterface {
    fun onItemClick(chatDetails : ChatMatchResult)
}