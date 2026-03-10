package com.chat.sms_text.messages.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.ActivitySpecificMessageDetailsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpecificMessageDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivitySpecificMessageDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_specific_message_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initClickListener()
    }

    private fun initView() {
        if (intent.hasExtra("MESSAGES_ID")) {
            val messageId = intent.getStringExtra("MESSAGES_ID")
            if (messageId != null) {
                if (areValidMessageIds(messageId)) {
                    binding.rvImagesView.visibility = View.GONE
                    binding.rvMessagesView.visibility = View.VISIBLE
                    getSmsDetails(this, messageId.toLong())
                } else {
                    binding.rvMessagesView.visibility = View.GONE
                    binding.rvImagesView.visibility = View.VISIBLE
                    val regex = Regex("^\\d+")
                    val messageIdForImage = regex.find(messageId)?.value ?: ""

                    val photoUri = "content://mms/part/$messageIdForImage".toUri()
                    Glide.with(this)
                        .load(photoUri)
                        .into(binding.ivImageDetails)

                    getMmsPartDetails(this, messageIdForImage.toLong())
                }
            }
        }
    }

    private fun areValidMessageIds(messageIds: String): Boolean {
        return !messageIds.trim().contains("(Images)", ignoreCase = true)
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun getSmsDetails(context: Context, messageId: Long) {
        val uri = "content://sms/".toUri()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.TYPE,
            Telephony.Sms.DATE,
        )

        val selection = "${Telephony.Sms._ID}=?"
        val selectionArgs = arrayOf(messageId.toString())

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val type = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val receivedTime = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))

                val contactName = getContactName(context, address)

                val formattedReceived = formatDate(receivedTime)

                val typeString = when (type) {
                    Telephony.Sms.MESSAGE_TYPE_INBOX -> "Inbox"
                    Telephony.Sms.MESSAGE_TYPE_SENT -> "Outbox"
                    Telephony.Sms.MESSAGE_TYPE_DRAFT -> "Draft"
                    else -> "Other"
                }

                binding.txtMessages.text = body
                binding.txtSenderName.text = contactName
                binding.txtSenderAddress.text = address
                binding.txtMessagesType.text = typeString
                binding.txtMessagesTitle.text = resources.getString(R.string.received)
                binding.txtReceivedTime.text = formattedReceived
            } else {
                Log.d("ABCD", "No message found for ID: $messageId")
            }
        }
    }

    private fun getMmsPartDetails(context: Context, partId: Long) {
        val uri = "content://mms/part/$partId".toUri()
        val projection = arrayOf(
            "_id",
            "mid",
        )

        val cursor = context.contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val mmsId = it.getLong(it.getColumnIndexOrThrow("mid"))

                getMmsDetails(context, mmsId)
            }
        }
    }

    private fun getMmsDetails(context: Context, mmsId: Long) {
        val uri = "content://mms/$mmsId".toUri()
        val projection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.DATE,
            Telephony.Mms.DATE_SENT,
            Telephony.Mms.MESSAGE_BOX
        )

        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val dateReceived = it.getLong(it.getColumnIndexOrThrow(Telephony.Mms.DATE)) * 1000L
                val msgBox = it.getInt(it.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX))

                val typeString = when (msgBox) {
                    Telephony.Mms.MESSAGE_BOX_INBOX -> "Inbox"
                    Telephony.Mms.MESSAGE_BOX_SENT -> "Outbox"
                    Telephony.Mms.MESSAGE_BOX_DRAFTS -> "Draft"
                    else -> "Other"
                }

                val formattedReceived = formatDate(dateReceived)

                val address = getMmsAddress(context, mmsId)
                val contactName = getContactName(context, address)

                binding.txtSenderName.text = contactName
                binding.txtSenderAddress.text = address
                binding.txtMessagesType.text = typeString
                binding.txtMessagesTitle.text = resources.getString(R.string.sent)
                binding.txtReceivedTime.text = formattedReceived
            }
        }
    }

    private fun getMmsAddress(context: Context, mmsId: Long): String {
        val uri = "content://mms/$mmsId/addr".toUri()
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address"),
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                if (!address.isNullOrEmpty() && address != "insert-address-token") {
                    return address
                }
            }
        }
        return "Unknown"
    }

    private fun getContactName(context: Context, phoneNumber: String?): String {
        if (phoneNumber == null) return "Unknown"
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val cursor = context.contentResolver.query(
            uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return "Unknown"
    }

    private fun formatDate(timeMillis: Long): String {
        if (timeMillis <= 0) return "N/A"
        val sdf = SimpleDateFormat("MMM dd, yyyy h:mm:ss a", Locale.ENGLISH)
        return sdf.format(Date(timeMillis))
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }
}