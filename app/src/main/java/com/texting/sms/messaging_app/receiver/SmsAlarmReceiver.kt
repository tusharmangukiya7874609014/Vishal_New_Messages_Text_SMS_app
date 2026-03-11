package com.texting.sms.messaging_app.receiver

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.ScheduledSMSDatabaseHelper
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.model.ChatModel
import androidx.core.net.toUri

class SmsAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id",-1)
        val number = intent.getStringExtra("number") ?: return
        val message = intent.getStringExtra("message") ?: return
        val selectedImageUri = intent.getStringExtra("selectedImageUri") ?: return

        sendSMSAndSimulateMMS(context, number, message, selectedImageUri)

        val dbHelper = ScheduledSMSDatabaseHelper(context)
        dbHelper.deleteSms(id)
    }

    private fun sendSMSAndSimulateMMS(
        context: Context,
        phoneNumber: String,
        message: String,
        selectedImageUris: String
    ) {
        val timestamp = System.currentTimeMillis()
        val newMessages = mutableListOf<ChatModel.MessageItem>()

        val restoredList = selectedImageUris.split(",").map { it.toUri() }.toMutableList()

        if (message.isNotBlank()) {
            try {
                val defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()

                val subscriptionId =
                    SharedPreferencesHelper.getInt(context, Const.SIM_SLOT_NUMBER, defaultSmsSubId)
                val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)

                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

                insertSmsSent(context, phoneNumber, message)

                newMessages.add(
                    ChatModel.MessageItem(
                        message = message,
                        timestamp = timestamp,
                        isFromMe = true,
                        isRead = true
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("ABCD", "Failed To Send SMS")
                return
            }
        }

        restoredList.forEach { uri ->
            newMessages.add(
                ChatModel.MessageItem(
                    message = "",
                    timestamp = timestamp,
                    isFromMe = true,
                    isRead = true,
                    mediaUri = uri
                )
            )
        }

        insertMmsSent(context, phoneNumber, restoredList)
    }

    private fun insertSmsSent(context: Context, phoneNumber: String, message: String) {
        val subscriptionId = SharedPreferencesHelper.getInt(
            context,
            Const.SIM_SLOT_NUMBER,
            SubscriptionManager.getDefaultSmsSubscriptionId()
        )

        val values = ContentValues().apply {
            put("address", phoneNumber)
            put("body", message)
            put("date", System.currentTimeMillis())
            put("read", 1)
            put("type", 2)
            put("sub_id", subscriptionId)
        }
        val uri = "content://sms/sent".toUri()
        context.contentResolver.insert(uri, values)
    }

    private fun insertMmsSent(context: Context, phoneNumber: String, imageUris: List<Uri>) {
        try {
            val threadId = getOrCreateThreadId(context, phoneNumber)
            val timestamp = System.currentTimeMillis() / 1000L

            val mmsValues = ContentValues().apply {
                put("thread_id", threadId)
                put("date", timestamp)
                put("read", 1)
                put("msg_box", 2) // Sent
                put("m_type", 128) // Sent
                put("ct_t", "application/vnd.wap.multipart.related")
                put("sub", "")
            }

            val mmsUri = context.contentResolver.insert("content://mms".toUri(), mmsValues)
                ?: throw Exception("Failed to insert MMS")

            val messageId = ContentUris.parseId(mmsUri)
            Log.d("ABCD", "Inserted MMS with ID: $messageId")

            // Add address
            val addrValues = ContentValues().apply {
                put("address", phoneNumber)
                put("type", 151) // 151 = To
                put("charset", 106)
            }
            context.contentResolver.insert("content://mms/$messageId/addr".toUri(), addrValues)

            // Add empty text part
            val textPartValues = ContentValues().apply {
                put("mid", messageId)
                put("ct", "text/plain")
                put("text", "")
                put("cid", "<text>")
                put("cl", "text.txt")
                put("seq", 0)
            }
            context.contentResolver.insert(
                "content://mms/$messageId/part".toUri(), textPartValues
            )

            // Add image parts
            imageUris.forEachIndexed { index, uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Cannot open URI: $uri")
                val bytes = inputStream.readBytes()
                inputStream.close()

                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

                val partValues = ContentValues().apply {
                    put("mid", messageId)
                    put("ct", mimeType)
                    put("cid", "<image$index>")
                    put("cl", "image$index.jpg")
                    put("name", "image$index.jpg")
                    put("seq", index + 1)
                }

                val partUri = context.contentResolver.insert(
                    "content://mms/$messageId/part".toUri(), partValues
                ) ?: throw Exception("Failed to insert MMS part")

                context.contentResolver.openOutputStream(partUri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
            Log.e("ABCD", "Error inserting MMS: ${e.localizedMessage}")
            e.printStackTrace()
        }
    }

    private fun getOrCreateThreadId(context: Context, phoneNumber: String): Long {
        val uri = "content://mms-sms/threadID".toUri().buildUpon()
            .appendQueryParameter("recipient", phoneNumber)
            .build()
        context.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        throw Exception("Failed to get or create thread ID")
    }
}