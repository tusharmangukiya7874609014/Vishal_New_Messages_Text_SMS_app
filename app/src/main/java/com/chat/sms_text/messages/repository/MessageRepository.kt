package com.chat.sms_text.messages.repository

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import com.chat.sms_text.messages.activity.HomeActivity.SmsPart
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.model.ChatUser
import com.chat.sms_text.messages.utils.MessageWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.chat.sms_text.messages.R
import kotlin.collections.iterator
import kotlin.collections.toMutableList

object MessageRepository {
    private var allSMSResponse: MutableList<ChatUser> = mutableListOf()
    private var firstSMSResponse: MutableList<ChatUser> = mutableListOf()
    private var remainingSMSResponse: MutableList<ChatUser> = mutableListOf()

    fun getLatestMessages(): List<ChatUser> {
        return allSMSResponse
    }

    fun fetchMessages(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {

            // Step 1: Load initial (first 15) threads
            val initialSmsChatList = getAllSmsThreads(context, 15)
            firstSMSResponse = initialSmsChatList.toMutableList()

            // Step 3: Merge and filter
            val mergedList = (firstSMSResponse + remainingSMSResponse)
                .distinctBy { it.threadId }

            val unarchivedThreads = SharedPreferencesHelper.filterUnarchivedThreads(
                context,
                mergedList
            )
            allSMSResponse = unarchivedThreads.toMutableList()

            withContext(Dispatchers.Main) {
                updateWidget(context)
            }

            // Step 2: Load all remaining threads
            val fullSmsChatList = getAllSmsThreads(context)
            remainingSMSResponse = fullSmsChatList.toMutableList()

            val mergedListSecond = (firstSMSResponse + remainingSMSResponse)
                .distinctBy { it.threadId }

            val unarchivedThreadsSeconds = SharedPreferencesHelper.filterUnarchivedThreads(
                context,
                mergedListSecond
            )

            allSMSResponse = unarchivedThreadsSeconds.toMutableList()

            withContext(Dispatchers.Main) {
                updateWidget(context)
            }
        }
    }

    private fun updateWidget(context: Context) {
        Log.e("Widget","updateWidget after load")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, MessageWidgetProvider::class.java)
        )
        appWidgetManager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list_view)

        for (id in ids) {
            MessageWidgetProvider.updateWidget(context, appWidgetManager, id)
        }
    }

    private fun getAllSmsThreads(context: Context, limit: Int = -1): List<ChatUser> {
        val smsThreads = mutableListOf<ChatUser>()
        val contentResolver = context.contentResolver

        val smsUri = "content://sms".toUri()
        val projection = arrayOf("thread_id", "address", "body", "date", "read", "sub_id", "type")
        val cursor = contentResolver.query(
            smsUri,
            projection,
            null,
            null,
            "date DESC"
        )

        val threadMap =
            mutableMapOf<Long, MutableList<SmsPart>>()
        val unreadCounter = mutableMapOf<Long, Int>()

        cursor?.use {
            while (it.moveToNext()) {
                val threadId = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                val address = it.getString(it.getColumnIndexOrThrow("address"))

                val body = it.getString(it.getColumnIndexOrThrow("body"))
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                val isRead = it.getInt(it.getColumnIndexOrThrow("read")) == 1
                val simSlot = try {
                    it.getInt(it.getColumnIndexOrThrow("sub_id"))
                } catch (_: Exception) {
                    -1
                }
                val type = it.getInt(it.getColumnIndexOrThrow("type"))

                if (!isRead) {
                    unreadCounter[threadId] = unreadCounter.getOrDefault(threadId, 0) + 1
                }

                val list = threadMap.getOrPut(threadId) { mutableListOf() }
                list.add(SmsPart(address, body, date, isRead, simSlot = simSlot, type))
            }
        }

        for ((threadId, messages) in threadMap) {
            val sortedMessages = messages.sortedByDescending { it.date }

            var latestMergedMessage: String
            var latestTimestamp = 0L
            val buffer = StringBuilder()
            var prevTime: Long? = null

            for (sms in sortedMessages) {
                if (prevTime == null || (prevTime - sms.date) <= 100) {
                    if (buffer.isEmpty()) latestTimestamp = sms.date
                    buffer.insert(0, sms.body)
                    prevTime = sms.date
                } else {
                    break
                }
            }

            val latestMessage = sortedMessages.first()
            latestMergedMessage = buffer.toString()
            val previewText = if (latestMessage.type == 2) {
                context.resources.getString(R.string.you, latestMergedMessage)
            } else {
                latestMergedMessage
            }

            val unreadCount = unreadCounter[threadId] ?: 0

            val contactInfo = getContactInfo(context, sortedMessages.first().address)
            val latestSimSlot = sortedMessages.first().simSlot
            smsThreads.add(
                ChatUser(
                    threadId = threadId,
                    latestMessage = previewText,
                    timestamp = latestTimestamp,
                    address = sortedMessages.first().address,
                    contactName = contactInfo.first,
                    photoUri = contactInfo.second,
                    unreadCount = unreadCount,
                    simSlot = latestSimSlot
                )
            )

            if (limit in 1..smsThreads.size) break
        }

        return smsThreads.sortedByDescending { it.timestamp }
    }

    private fun getContactInfo(context: Context, phoneNumber: String): Pair<String?, String?> {
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUri =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                return Pair(name, photoUri)
            }
        }

        return Pair(null, null)
    }
}
