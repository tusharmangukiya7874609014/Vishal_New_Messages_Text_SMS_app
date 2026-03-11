package com.texting.sms.messaging_app.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactNameCache {
    private val cache = mutableMapOf<String, String?>()

    fun getName(phoneNumber: String): String? {
        return cache[phoneNumber]
    }

    fun putName(phoneNumber: String, name: String?) {
        cache[phoneNumber] = name
    }

    fun extractName(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                )
            }
        }
        return null
    }
}