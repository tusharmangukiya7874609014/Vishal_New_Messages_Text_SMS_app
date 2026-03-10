package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.AppLanguage

interface LanguageInterface {
    fun onItemClick(language: AppLanguage, position: Int)
}