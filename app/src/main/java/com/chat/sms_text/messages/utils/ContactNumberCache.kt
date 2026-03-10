package com.chat.sms_text.messages.utils

object ContactNumberCache {
    private val cache = HashMap<String, String>()

    fun getNumber(contactId: String): String? = cache[contactId]

    fun putNumber(contactId: String, number: String) {
        cache[contactId] = number
    }
}