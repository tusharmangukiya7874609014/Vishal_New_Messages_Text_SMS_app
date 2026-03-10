package com.chat.sms_text.messages.utils

import android.content.Context
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.ContactsContract

object ProfileCache {

    private val photoCache = mutableMapOf<String, String?>()

    fun getPhoto(address: String): String? {
        return photoCache[address]
    }

    fun getOrLoadPhoto(context: Context, address: String): String? {
        return photoCache[address] ?: run {
            val photoUri = getContactPhoto(context, address)
            photoCache[address] = photoUri
            photoUri
        }
    }

    fun updatePhoto(context: Context, address: String): String? {
        val photoUri = getContactPhoto(context, address)
        photoCache[address] = photoUri
        return photoUri
    }

    fun isAddressBlocked(context: Context, address: String): Boolean {
        return try {
            BlockedNumberContract.isBlocked(context, address)
        } catch (_: Exception) {
            false
        }
    }

    private fun getContactPhoto(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(ContactsContract.PhoneLookup.PHOTO_URI)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                )
            }
        }
        return null
    }
}