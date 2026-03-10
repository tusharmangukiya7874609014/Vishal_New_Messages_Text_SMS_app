package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.response.Language

interface SelectLanguageInterface {
    fun onItemClick(language: Language)
}