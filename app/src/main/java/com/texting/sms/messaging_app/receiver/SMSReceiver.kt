package com.texting.sms.messaging_app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsMessage
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.activity.HomeActivity
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.model.ChatMessage
import com.texting.sms.messaging_app.utils.ChatsStoreSMS
import java.util.Locale

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle: Bundle? = intent.extras
            try {
                if (bundle != null) {
                    val pdus = bundle.get("pdus") as Array<*>
                    val messages: MutableList<SmsMessage> = mutableListOf()
                    for (pdu in pdus) {
                        val format = bundle.getString("format")
                        val sms = SmsMessage.createFromPdu(pdu as ByteArray, format)
                        messages.add(sms)
                    }

                    var lastTimestamp = 0L
                    var mergedBody = ""
                    var senderAddress: String? = null

                    for (message in messages) {
                        val sender = message.displayOriginatingAddress
                        val body = message.messageBody
                        val currentTime = System.currentTimeMillis()

                        if (sender == senderAddress && (currentTime - lastTimestamp) <= 1000) {
                            mergedBody += body
                        } else {
                            if (mergedBody.isNotBlank() && senderAddress != null) {
                                context?.let {
                                    if (isDefaultSmsApp(it)) {
                                        val threadID =
                                            getThreadIdForAddress(it, senderAddress)
                                        val (name, number) = getContactInfoFromThreadId(
                                            it,
                                            threadID
                                        )
                                        val photoUri = getContactPhotoUriFromThreadId(it, threadID)

                                        insertSmsInbox(it, senderAddress, mergedBody)

                                        val notificationButtonOne =
                                            SharedPreferencesHelper.getString(
                                                context = it,
                                                Const.NOTIFICATION_BUTTON_ONE,
                                                it.resources.getString(R.string.none)
                                            )
                                        val notificationButtonTwo =
                                            SharedPreferencesHelper.getString(
                                                it,
                                                Const.NOTIFICATION_BUTTON_TWO,
                                                it.resources.getString(R.string.mark_read)
                                            )
                                        val notificationButtonThree =
                                            SharedPreferencesHelper.getString(
                                                it,
                                                Const.NOTIFICATION_BUTTON_THREE,
                                                it.resources.getString(R.string.reply)
                                            )

                                        val isPrivateThread =
                                            SharedPreferencesHelper.isThreadPrivate(
                                                context,
                                                threadID
                                            )
                                        val privateNotification =
                                            SharedPreferencesHelper.getBoolean(
                                                context,
                                                Const.PRIVATE_CHAT_NOTIFICATION,
                                                true
                                            )

                                        if (isPrivateThread) {
                                            if (privateNotification) {
                                                showSMSNotification(
                                                    it,
                                                    threadID,
                                                    name ?: senderAddress,
                                                    mergedBody,
                                                    number ?: "",
                                                    photoUri,
                                                    buttonActions = listOf(
                                                        notificationButtonOne,
                                                        notificationButtonTwo,
                                                        notificationButtonThree
                                                    )
                                                )
                                            }
                                        } else {
                                            showSMSNotification(
                                                it,
                                                threadID,
                                                name ?: senderAddress,
                                                mergedBody,
                                                number ?: "",
                                                photoUri,
                                                buttonActions = listOf(
                                                    notificationButtonOne,
                                                    notificationButtonTwo,
                                                    notificationButtonThree
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            senderAddress = sender
                            mergedBody = body
                        }

                        lastTimestamp = currentTime
                    }

                    if (mergedBody.isNotBlank() && senderAddress != null) {
                        context?.let {
                            if (isDefaultSmsApp(it)) {
                                val threadID = getThreadIdForAddress(it, senderAddress)
                                val (name, number) = getContactInfoFromThreadId(it, threadID)

                                val photoUri = getContactPhotoUriFromThreadId(it, threadID)

                                insertSmsInbox(it, senderAddress, mergedBody)

                                val notificationButtonOne = SharedPreferencesHelper.getString(
                                    it,
                                    Const.NOTIFICATION_BUTTON_ONE,
                                    it.resources.getString(R.string.none)
                                )
                                val notificationButtonTwo = SharedPreferencesHelper.getString(
                                    it,
                                    Const.NOTIFICATION_BUTTON_TWO,
                                    it.resources.getString(R.string.mark_read)
                                )
                                val notificationButtonThree = SharedPreferencesHelper.getString(
                                    it,
                                    Const.NOTIFICATION_BUTTON_THREE,
                                    it.resources.getString(R.string.reply)
                                )

                                val isPrivateThread =
                                    SharedPreferencesHelper.isThreadPrivate(context, threadID)
                                val privateNotification = SharedPreferencesHelper.getBoolean(
                                    context,
                                    Const.PRIVATE_CHAT_NOTIFICATION,
                                    true
                                )

                                if (isPrivateThread) {
                                    if (privateNotification) {
                                        showSMSNotification(
                                            it,
                                            threadID,
                                            name ?: senderAddress,
                                            mergedBody,
                                            number ?: "",
                                            photoUri,
                                            buttonActions = listOf(
                                                notificationButtonOne,
                                                notificationButtonTwo,
                                                notificationButtonThree
                                            )
                                        )
                                    }
                                } else {
                                    showSMSNotification(
                                        it,
                                        threadID,
                                        name ?: senderAddress,
                                        mergedBody,
                                        number ?: "",
                                        photoUri,
                                        buttonActions = listOf(
                                            notificationButtonOne,
                                            notificationButtonTwo,
                                            notificationButtonThree
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ABCD", "Exception in onReceive: ${e.message}")
            }
        }
    }

    private fun showSMSNotification(
        context: Context,
        threadId: Long,
        sender: String,
        newMessage: String,
        phoneNumber: String,
        senderPhotoUri: Uri?,
        isReply: Boolean = false,
        buttonActions: List<String>
    ) {
        val notificationPreview = SharedPreferencesHelper.getString(
            context,
            Const.NOTIFICATIONS_PREVIEW,
            context.resources.getString(R.string.show_name_message)
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            threadId.toString(), sender,
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(channel)
        // Add new message to store
        val msgList = ChatsStoreSMS.messageMap.getOrPut(threadId) { mutableListOf() }
        msgList.add(ChatMessage(newMessage, System.currentTimeMillis(), isReply))

        // Create Person
        val senderIcon = getRoundedIcon(context, senderPhotoUri)
        val senderPerson = when (notificationPreview) {
            context.resources.getString(R.string.show_name_message) -> {
                Person.Builder()
                    .setName(sender)
                    .setIcon(senderIcon)
                    .build()
            }

            context.resources.getString(R.string.show_name) -> {
                Person.Builder()
                    .setName(sender)
                    .setIcon(senderIcon)
                    .build()
            }

            context.resources.getString(R.string.hide_contents) -> {
                Person.Builder()
                    .setName(context.resources.getString(R.string.app_name))
                    .setIcon(senderIcon)
                    .build()
            }

            else -> {
                Person.Builder()
                    .setName(sender)
                    .setIcon(senderIcon)
                    .build()
            }
        }

        val mePerson = Person.Builder()
            .setName("You")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_profile))
            .build()

        // Messaging style
        val messagingStyle = NotificationCompat.MessagingStyle(mePerson)

        when (notificationPreview) {
            context.resources.getString(R.string.show_name_message) -> {
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
            }

            context.resources.getString(R.string.show_name) -> {
                val messageCount = msgList.count { !it.isMe }
                val previewText = if (messageCount > 1) {
                    "$messageCount new message"
                } else {
                    "New message"
                }
                messagingStyle.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        previewText,
                        System.currentTimeMillis(),
                        senderPerson
                    )
                )
            }

            context.resources.getString(R.string.hide_contents) -> {
                val messageCount = msgList.count { !it.isMe }
                val previewText = if (messageCount > 1) {
                    "$messageCount new message"
                } else {
                    "New message"
                }
                messagingStyle.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        previewText,
                        System.currentTimeMillis(),
                        senderPerson
                    )
                )
            }
        }

        // Intent to open app
        val contentIntent = PendingIntent.getActivity(
            context, 0, Intent(context, HomeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, threadId.toString())
            .setSmallIcon(R.drawable.ic_message_fab)
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        buttonActions.forEach { actionType ->
            buildAction(context, actionType, threadId, phoneNumber)?.let { builder.addAction(it) }
        }

        notificationManager.notify(threadId.toInt(), builder.build())
    }

    private fun buildAction(
        context: Context,
        type: String,
        threadId: Long,
        phoneNumber: String
    ): NotificationCompat.Action? {

        return when (type) {
            context.resources.getString(R.string.mark_read) -> {
                // Mark as Read
                val readIntent = Intent(context, SMSActionReceiver::class.java).apply {
                    action = "ACTION_MARK_AS_READ"
                    putExtra("threadID", threadId)
                }

                val readPendingIntent = PendingIntent.getBroadcast(
                    context, 1, readIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                NotificationCompat.Action.Builder(
                    R.drawable.ic_read, "Mark as Read", readPendingIntent
                ).build()
            }

            context.resources.getString(R.string.reply) -> {
                // Reply Action
                val remoteInput = RemoteInput.Builder("key_text_reply")
                    .setLabel("Reply")
                    .build()

                val replyIntent = Intent(context, SMSActionReceiver::class.java).apply {
                    action = "ACTION_INLINE_REPLY"
                    putExtra("sender", phoneNumber)
                    putExtra("threadID", threadId)
                }

                val replyPendingIntent = PendingIntent.getBroadcast(
                    context, 2, replyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                NotificationCompat.Action.Builder(
                    R.drawable.ic_message_fab, "Reply", replyPendingIntent
                ).addRemoteInput(remoteInput).build()
            }

            context.resources.getString(R.string.call) -> {
                val callIntent = Intent(context, SMSActionReceiver::class.java).apply {
                    action = "ACTION_CALL"
                    putExtra("threadID", threadId)
                }
                val callPendingIntent = PendingIntent.getBroadcast(
                    context, 3, callIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                NotificationCompat.Action.Builder(
                    R.drawable.ic_call, "Call", callPendingIntent
                ).build()
            }

            context.resources.getString(R.string.delete) -> {
                val deleteIntent = Intent(context, SMSActionReceiver::class.java).apply {
                    action = "ACTION_DELETE"
                    putExtra("threadID", threadId)
                }

                val deletePendingIntent = PendingIntent.getBroadcast(
                    context, 4, deleteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                NotificationCompat.Action.Builder(
                    R.drawable.ic_delete, "Delete", deletePendingIntent
                ).build()
            }

            else -> null
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

    private fun getThreadIdForAddress(context: Context, address: String): Long {
        val uri = "content://sms".toUri()
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("thread_id"),
            "address = ?",
            arrayOf(address),
            "date DESC"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow("thread_id"))
            }
        }
        return -1L
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
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                return name
            }
        }

        return null
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

    private fun isDefaultSmsApp(context: Context): Boolean {
        return context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
    }

    private fun insertSmsInbox(context: Context, phoneNumber: String, message: String) {
        try {
            val uri = "content://sms/inbox".toUri()

            // Query last message from the same number
            val cursor = context.contentResolver.query(
                uri,
                arrayOf("_id", "body", "date"),
                "address = ?",
                arrayOf(phoneNumber),
                "date DESC LIMIT 1"
            )

            val currentTime = System.currentTimeMillis()

            var lastMessageId: Long? = null
            var lastBody: String? = null
            var lastTimestamp: Long? = null

            cursor?.use {
                if (it.moveToFirst()) {
                    lastMessageId = it.getLong(it.getColumnIndexOrThrow("_id"))
                    lastBody = it.getString(it.getColumnIndexOrThrow("body"))
                    lastTimestamp = it.getLong(it.getColumnIndexOrThrow("date"))
                }
            }

            if (lastMessageId != null && (currentTime - lastTimestamp!!) < 1000) {
                val updatedBody = "$lastBody $message"
                val updateValues = ContentValues().apply {
                    put("body", updatedBody.trim())
                    put("date", currentTime)
                }

                val updateUri = Uri.withAppendedPath(uri, lastMessageId.toString())
                val updatedRows =
                    context.contentResolver.update(updateUri, updateValues, null, null)

                Log.d("ABCD", "Updated SMS ID $lastMessageId, rows affected: $updatedRows")
            } else {
                val values = ContentValues().apply {
                    put("address", phoneNumber)
                    put("body", message)
                    put("date", currentTime)
                    put("read", 0)
                    put("type", 1)
                }

                context.contentResolver.insert(uri, values)
            }
        } catch (e: Exception) {
            Log.e("ABCD", "Insert/Update failed: ${e.message}")
        }
    }
}