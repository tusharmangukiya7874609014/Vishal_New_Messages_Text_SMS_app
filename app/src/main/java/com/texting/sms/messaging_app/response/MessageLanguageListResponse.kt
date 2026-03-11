package com.texting.sms.messaging_app.response

data class MessageLanguageListResponse(
    val languages: List<Language>,
    val status: Boolean
)