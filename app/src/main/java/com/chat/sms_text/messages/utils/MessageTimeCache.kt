package com.chat.sms_text.messages.utils

import android.icu.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MessageTimeCache {
    private val cache = mutableMapOf<Long, String>()

    fun getFormattedTime(timestamp: Long): String? {
        return cache[timestamp]
    }

    fun putFormattedTime(timestamp: Long, formatted: String) {
        cache[timestamp] = formatted
    }

    fun formatMessageTime(timestamp: Long): String {
        val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when {
            now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == messageCal.get(
                Calendar.DAY_OF_YEAR
            ) -> {
                SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(timestamp))
            }

            now.get(Calendar.WEEK_OF_YEAR) == messageCal.get(Calendar.WEEK_OF_YEAR) && now.get(
                Calendar.YEAR
            ) == messageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(timestamp))
            }

            now.get(Calendar.MONTH) == messageCal.get(Calendar.MONTH) && now.get(Calendar.YEAR) == messageCal.get(
                Calendar.YEAR
            ) -> {
                SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(timestamp))
            }

            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(timestamp))
            }
        }
    }
}