package com.chat.sms_text.messages.response

data class MessageLanguageListResponse(
    val languages: List<Language>,
    val status: Boolean
)