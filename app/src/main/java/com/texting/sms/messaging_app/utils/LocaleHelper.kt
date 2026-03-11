package com.texting.sms.messaging_app.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, language: String) {
        updateResources(context, language)
    }

    private fun updateResources(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.resources.updateConfiguration(config,context.resources.displayMetrics)
    }
}