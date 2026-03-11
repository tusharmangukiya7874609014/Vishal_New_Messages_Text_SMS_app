package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.response.Language

interface SelectLanguageInterface {
    fun onItemClick(language: Language)
}