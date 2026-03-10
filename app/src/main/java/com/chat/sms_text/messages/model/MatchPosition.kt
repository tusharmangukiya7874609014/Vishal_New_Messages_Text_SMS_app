package com.chat.sms_text.messages.model

data class MatchPosition(
    val messageIndex: Int,
    val startIndex: Int,
    val endIndex: Int
)