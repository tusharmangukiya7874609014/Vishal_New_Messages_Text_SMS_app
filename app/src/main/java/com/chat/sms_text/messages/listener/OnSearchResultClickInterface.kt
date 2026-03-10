package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ChatMatchResult

interface OnSearchResultClickInterface {
    fun onItemClick(chatDetails : ChatMatchResult)
}