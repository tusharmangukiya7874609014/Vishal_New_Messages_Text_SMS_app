package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.AppLanguage

interface LanguageInterface {
    fun onItemClick(language: AppLanguage, position: Int)
}