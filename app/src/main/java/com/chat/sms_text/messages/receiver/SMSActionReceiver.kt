package com.chat.sms_text.messages.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.activity.HomeActivity
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.model.ChatMessage
import com.chat.sms_text.messages.utils.ChatsStoreSMS
import java.util.Locale

class SMSActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sender = intent.getStringExtra("sender")
        val threadId = intent.getLongExtra("threadID", -1L)

        Log.d("ABCD", "Sender: $sender, Thread ID: $threadId")

        when (intent.action) {
            "ACTION_MARK_AS_READ" -> {
                val contentValues = ContentValues().apply {
                    put("read", 1)
                }

                val uri = "content://sms/inbox".toUri()
                val selection = "thread_id = ? AND read = 0"
                val selectionArgs = arrayOf(threadId.toString())

                try {
                    context.contentResolver.update(uri, contentValues, selection, selectionArgs)
                } catch (e: Exception) {
                    Log.e("SMS_ACTION", "Failed to mark messages as read", e)
                }

                ChatsStoreSMS.messageMap.remove(threadId)
                notificationManager.cancel(threadId.toInt())
            }

            "ACTION_INLINE_REPLY" -> {
                val notificationPreview = SharedPreferencesHelper.getString(
                    context,
                    Const.NOTIFICATIONS_PREVIEW,
                    context.resources.getString(R.string.show_name_message)
                )
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence("key_text_reply")?.toString()

                when (notificationPreview) {
                    context.resources.getString(R.string.show_name_message) -> {
                        if (!replyText.isNullOrEmpty()) {
                            if (sender != null) {
                                val (name, _) = getContactInfoFromThreadId(
                                    context,
                                    threadId
                                )
                                val photoUri = getContactPhotoUriFromThreadId(context, threadId)

                                showSMSNotification(
                                    context = context,
                                    threadId = threadId,
                                    sender = sender,
                                    newMessage = replyText,
                                    phoneNumber = name ?: sender,
                                    senderPhotoUri = photoUri
                                )
                            }

                            sendSms(context, sender ?: "", replyText)
                        }
                    }

                    context.resources.getString(R.string.show_name) -> {
                        if (!replyText.isNullOrEmpty()) {
                            sendSms(context, sender ?: "", replyText)
                            ChatsStoreSMS.messageMap.remove(threadId)
                            notificationManager.cancel(threadId.toInt())
                        }
                    }

                    context.resources.getString(R.string.hide_contents) -> {
                        if (!replyText.isNullOrEmpty()) {
                            sendSms(context, sender ?: "", replyText)
                            ChatsStoreSMS.messageMap.remove(threadId)
                            notificationManager.cancel(threadId.toInt())
                        }
                    }
                }
            }

            "ACTION_CALL" -> {
                val (_, number) = getContactInfoFromThreadId(
                    context,
                    threadId
                )
                val sanitizedNumber = number?.replace(Regex("[^\\d+]"), "")

                if (sanitizedNumber?.isNotEmpty() == true) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:$number".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialIntent)

                        ChatsStoreSMS.messageMap.remove(threadId)
                        notificationManager.cancel(threadId.toInt())
                        notificationManager.cancelAll()
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to start dialer", e)
                    }
                }
            }

            "ACTION_DELETE" -> {
                try {
                    val uri = "content://sms".toUri()
                    val projection = arrayOf("_id")
                    val selection = "thread_id = ?"
                    val selectionArgs = arrayOf(threadId.toString())
                    val sortOrder = "date DESC LIMIT 1"

                    val cursor = context.contentResolver.query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                    )

                    cursor?.use {
                        if (it.moveToFirst()) {
                            val messageId = it.getLong(it.getColumnIndexOrThrow("_id"))
                            val deleteUri = "content://sms/$messageId".toUri()
                            val deleted = context.contentResolver.delete(deleteUri, null, null)

                            Log.d("SMS_ACTION", "Deleted message ID: $messageId, result: $deleted")

                            ChatsStoreSMS.messageMap[threadId]?.removeIf { msg -> msg.timestamp == messageId }

                            if (ChatsStoreSMS.messageMap[threadId]?.isEmpty() == true) {
                                ChatsStoreSMS.messageMap.remove(threadId)
                            }
                            notificationManager.cancel(threadId.toInt())                        }
                    }
                } catch (e: Exception) {
                    Log.e("SMS_ACTION", "Failed to delete message", e)
                }
            }
        }
    }

    private fun getContactPhotoUriFromThreadId(context: Context, threadId: Long): Uri? {
        // Step 1: Get address (phone number) from thread ID
        val uri = "content://sms/inbox".toUri()
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address"),
            "thread_id = ?",
            arrayOf(threadId.toString()),
            "date DESC LIMIT 1"
        )

        var phoneNumber: String? = null
        cursor?.use {
            if (it.moveToFirst()) {
                phoneNumber = it.getString(it.getColumnIndexOrThrow("address"))
            }
        }

        if (phoneNumber.isNullOrEmpty()) return null

        // Step 2: Normalize phone number (optional but improves matching)
        val normalizedNumber = PhoneNumberUtils.formatNumberToE164(
            phoneNumber, Locale.ENGLISH.country
        ) ?: phoneNumber

        // Step 3: Query Contacts for photo URI using the number
        val contactUri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalizedNumber)
        )

        val contactCursor = context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.PhoneLookup.PHOTO_URI
            ),
            null,
            null,
            null
        )

        var photoUri: Uri? = null
        contactCursor?.use {
            if (it.moveToFirst()) {
                val photoUriStr =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                if (!photoUriStr.isNullOrEmpty()) {
                    photoUri = photoUriStr.toUri()
                }
            }
        }

        return photoUri
    }

    private fun getContactInfoFromThreadId(
        context: Context,
        threadId: Long
    ): Pair<String?, String?> {
        val phoneNumber = getPhoneNumberFromThreadId(context, threadId)
        val contactName = phoneNumber?.let { getContactName(context, it) }
        return Pair(contactName, phoneNumber)
    }

    private fun getPhoneNumberFromThreadId(context: Context, threadId: Long): String? {
        val uri = "content://sms".toUri()
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address"),
            "thread_id = ?",
            arrayOf(threadId.toString()),
            "date DESC"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow("address"))
            }
        }
        return null
    }

    private fun getContactName(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return null
    }

    private fun showSMSNotification(
        context: Context,
        threadId: Long,
        sender: String,
        newMessage: String,
        phoneNumber: String,
        senderPhotoUri: Uri?,
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Notification channel
        val channel = NotificationChannel(
            threadId.toString(), sender,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // Add new message to store
        val msgList = ChatsStoreSMS.messageMap.getOrPut(threadId) { mutableListOf() }
        msgList.add(ChatMessage(newMessage, System.currentTimeMillis(), true))

        // Create Person
        val senderIcon = getRoundedIcon(context, senderPhotoUri)
        val senderPerson = Person.Builder()
            .setName(phoneNumber)
            .setIcon(senderIcon)
            .build()

        val mePerson = Person.Builder()
            .setName("You")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_profile))
            .build()

        // Messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)

        for (msg in msgList) {
            val person = if (msg.isMe) mePerson else senderPerson
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    msg.message,
                    msg.timestamp,
                    person
                )
            )
        }

        // Intent to open app
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Reply Action
        val remoteInput = androidx.core.app.RemoteInput.Builder("key_text_reply")
            .setLabel("Reply")
            .build()

        val replyIntent = Intent(context, SMSActionReceiver::class.java).apply {
            action = "ACTION_INLINE_REPLY"
            putExtra("sender", phoneNumber)
            putExtra("threadID", threadId)
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            context, 1, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_message_fab, "Reply", replyPendingIntent
        ).addRemoteInput(remoteInput).build()

        // Mark as Read
        val readIntent = Intent(context, SMSActionReceiver::class.java).apply {
            action = "ACTION_MARK_AS_READ"
            putExtra("threadID", threadId)
        }

        val readPendingIntent = PendingIntent.getBroadcast(
            context, 2, readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val readAction = NotificationCompat.Action.Builder(
            R.drawable.ic_read, "Mark as Read", readPendingIntent
        ).build()

        // Build notification
        val builder = NotificationCompat.Builder(context, threadId.toString())
            .setSmallIcon(R.drawable.ic_message_fab)
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(replyAction)
            .addAction(readAction)

        notificationManager.notify(threadId.toInt(), builder.build())
    }

    private fun getRoundedIcon(context: Context, uri: Uri?): IconCompat? {
        return try {
            val stream = uri?.let { context.contentResolver.openInputStream(it) }
            val bitmap = BitmapFactory.decodeStream(stream)
            val rounded = createBitmap(bitmap.width, bitmap.height)
            val canvas = Canvas(rounded)
            val paint = Paint().apply {
                isAntiAlias = true
                shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            val radius = bitmap.width.coerceAtMost(bitmap.height) / 2f
            canvas.drawCircle(bitmap.width / 2f, bitmap.height / 2f, radius, paint)
            IconCompat.createWithBitmap(rounded)
        } catch (_: Exception) {
            null
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()

            val subscriptionId =
                SharedPreferencesHelper.getInt(context, Const.SIM_SLOT_NUMBER, defaultSmsSubId)
            val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

            insertSmsSent(context, phoneNumber, message)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
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
            put("read", 0)
            put("type", 2)
            put("sub_id", subscriptionId)
        }
        val uri = "content://sms/sent".toUri()
        context.contentResolver.insert(uri, values)
    }
}
