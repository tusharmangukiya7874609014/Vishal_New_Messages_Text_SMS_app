package com.chat.sms_text.messages.model

data class AppLanguage(
    val countryName: String? = null,
    val exampleName: String? = null,
    val languageCode: String? = null,
    var isLanguageSelected: Boolean = false
)