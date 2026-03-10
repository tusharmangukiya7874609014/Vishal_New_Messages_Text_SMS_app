package com.chat.sms_text.messages.model

import androidx.appcompat.app.AppCompatDelegate

enum class AppTheme(val mode: Int) {
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES)
}

