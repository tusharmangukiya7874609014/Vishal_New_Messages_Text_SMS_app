package com.texting.sms.messaging_app.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.role.RoleManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.ForwardMessageListAdapter
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivitySelectRecipientsBinding
import com.texting.sms.messaging_app.listener.OnRemoveForwardRecipients
import com.texting.sms.messaging_app.listener.OnSelectedMessageRemove
import com.texting.sms.messaging_app.model.ContactModel
import com.texting.sms.messaging_app.utils.ContactNumberCache
import com.texting.sms.messaging_app.utils.LocaleHelper
import com.texting.sms.messaging_app.adapter.ForwardRecipientsAdapter
import com.texting.sms.messaging_app.adapter.SelectRecipientsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectRecipientsActivity : AppCompatActivity(), OnRemoveForwardRecipients,
    OnSelectedMessageRemove {
    private lateinit var binding: ActivitySelectRecipientsBinding
    private lateinit var rvAllContactListAdapter: SelectRecipientsAdapter
    private lateinit var rvForwardRecipientsAdapter: ForwardRecipientsAdapter
    private lateinit var rvForwardMessageListAdapter: ForwardMessageListAdapter
    private lateinit var allContactsList: MutableList<ContactModel>
    private lateinit var forwardRecipientsList: MutableList<ContactModel.ContactItem>
    private var storeMobileList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_select_recipients)
        initView()
        initClickListener()
    }

    override fun attachBaseContext(newBase: Context) {
        val scale = SharedPreferencesHelper.getFontScale(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = scale

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun getBaseContext(): Context {
        val locale = SharedPreferencesHelper.getLanguage(applicationContext)
        LocaleHelper.setLocale(this, locale)
        return super.getBaseContext()
    }

    private fun initView() {
        storeMobileList.clear()
        forwardRecipientsList = ArrayList()

        val selectedMessagesIds =
            SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

        val layoutManagerSelectedMessage: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvForwardMessageList.setLayoutManager(layoutManagerSelectedMessage)
        rvForwardMessageListAdapter = ForwardMessageListAdapter(
            selectedMessagesIds,
            this,
            this,
        )
        binding.rvForwardMessageList.adapter = rvForwardMessageListAdapter
        (binding.rvForwardMessageList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvAllContactsList.setLayoutManager(layoutManager)
        rvAllContactListAdapter = SelectRecipientsAdapter(
            mutableListOf(),
            storeMobileList,
            this,
        )
        binding.rvAllContactsList.adapter = rvAllContactListAdapter
        (binding.rvAllContactsList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        val layoutManagerOfSelected: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvForwardRecipients.setLayoutManager(layoutManagerOfSelected)
        rvForwardRecipientsAdapter = ForwardRecipientsAdapter(
            mutableListOf(),
            this,
        )
        binding.rvForwardRecipients.adapter = rvForwardRecipientsAdapter
        (binding.rvForwardRecipients.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                allContactsList =
                    getGroupedContacts(this@SelectRecipientsActivity)

                withContext(Dispatchers.Main) {
                    if (allContactsList.isNotEmpty()) {
                        binding.rvNoFavouriteContacts.visibility = View.GONE
                        binding.rvAllContactsList.fadeIn()
                        rvAllContactListAdapter.updateData(allContactsList)
                        binding.paginationProgress.fadeOut()
                        binding.rvAllContactsList.alpha = 1f
                    } else {
                        binding.paginationProgress.fadeOut()
                        binding.rvAllContactsList.visibility = View.GONE
                        binding.rvNoFavouriteContacts.fadeIn()
                    }
                }
            }
        }
    }

    fun updateSelectedCount(selectedCount: Int) {
        if (selectedCount != 0) {
            forwardRecipientsList.clear()
            val selectedMobileList =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MOBILE_LIST)
            selectedMobileList.forEach {
                if (forwardRecipientsList.none { recipient ->
                        recipient.phoneNumbers?.contains(it)
                            ?: false
                    }) {
                    val contactDetails = getContactByPhoneNumber(this, it)
                    if (contactDetails != null) {
                        forwardRecipientsList.add(contactDetails)
                    }
                }
            }
            rvForwardRecipientsAdapter.updateData(forwardRecipientsList)
            val layoutManager = binding.rvForwardRecipients.layoutManager as LinearLayoutManager
            binding.rvForwardRecipients.post {
                layoutManager.scrollToPositionWithOffset(
                    binding.rvForwardRecipients.adapter?.itemCount?.minus(1) ?: 0,
                    0
                )
            }
            binding.rvForwardView.fadeIn()
        } else {
            binding.rvForwardView.fadeOut()
        }
    }

    private fun getContactByPhoneNumber(
        context: Context,
        phoneNumber: String
    ): ContactModel.ContactItem? {
        val contentResolver = context.contentResolver

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUri =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))

                val numbers = mutableListOf<String>()
                val phonesCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )

                phonesCursor?.use { pc ->
                    while (pc.moveToNext()) {
                        val number =
                            pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        numbers.add(number)
                    }
                }

                return ContactModel.ContactItem(
                    contactId = contactId,
                    name = name,
                    phoneNumbers = numbers[0],
                    photoUri = photoUri
                )
            }
        }
        return null
    }

    private fun View.fadeIn(duration: Long = 300) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(null)
        }
    }

    private fun View.fadeOut(duration: Long = 300) {
        if (isVisible) {
            animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                    }
                })
        }
    }

    private fun getGroupedContacts(
        context: Context
    ): MutableList<ContactModel> {

        val contactItems: List<ContactModel.ContactItem> = getAllContactsSuspend(context)

        val grouped = contactItems.groupBy { it.name?.firstOrNull()?.uppercaseChar() ?: '#' }
        val finalList = mutableListOf<ContactModel>()

        grouped.toSortedMap().forEach { (initial, group) ->
            if (group.isNotEmpty()) {
                finalList.add(ContactModel.HeaderWithFirstItem(initial.toString(), group[0]))
                finalList.addAll(group.drop(1))
            }
        }

        return finalList
    }

    private fun getAllContactsSuspend(
        context: Context
    ): List<ContactModel.ContactItem> {
        val contactsList = mutableListOf<ContactModel.ContactItem>()

        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_URI
            ), null, null, "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val photoIndex = it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)

            while (it.moveToNext()) {
                val contactId = it.getString(idIndex)
                val name = it.getString(nameIndex)
                val photoUri = it.getString(photoIndex)

                if (!name.isNullOrBlank()) {
                    val phoneNumberList = ContactNumberCache
                        .getNumber(contactId)

                    contactsList.add(
                        ContactModel.ContactItem(
                            contactId,
                            name,
                            phoneNumberList,
                            photoUri
                        )
                    )
                }
            }
        }

        return contactsList
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivKeypad.setOnClickListener {
            binding.ivKeypad.visibility = View.GONE
            binding.ivKeyboard.fadeIn()
            binding.etSearchContacts.inputType = InputType.TYPE_CLASS_NUMBER
            binding.etSearchContacts.keyListener = DigitsKeyListener.getInstance("0123456789")
            binding.etSearchContacts.requestFocus()

            binding.etSearchContacts.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etSearchContacts, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.ivKeyboard.setOnClickListener {
            binding.ivKeyboard.visibility = View.GONE
            binding.ivKeypad.fadeIn()
            binding.etSearchContacts.inputType = InputType.TYPE_CLASS_TEXT
            binding.etSearchContacts.requestFocus()

            binding.etSearchContacts.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.etSearchContacts, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        binding.etSearchContacts.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    filterContactsWithHeaders(s.toString())
                } else {
                    filterContactsWithHeaders("", true)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ivClose.visibility = if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }
        })

        binding.ivClose.setOnClickListener {
            binding.etSearchContacts.text.clear()
        }

        binding.rvSendMessage.setOnClickListener {
            if (forwardRecipientsList.isEmpty()) {
                Toast.makeText(this, getString(R.string.no_recipients_selected), Toast.LENGTH_SHORT)
                    .show()
            } else {
                hideKeyboard()
                forwardRecipientsList.forEach { contact ->
                    sendSMSAndSimulateMMS(contact.phoneNumbers.toString())
                }
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun Activity.hideKeyboard() {
        val view = currentFocus ?: View(this)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getSmsBodyById(context: Context, messageId: String): String? {
        val uri = Uri.withAppendedPath("content://sms".toUri(), messageId)
        val projection = arrayOf("body")

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("body"))
            }
        }
        return null
    }

    private fun sendSMSAndSimulateMMS(phoneNumber: String) {
        val selectedMessagesIds =
            SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

        selectedMessagesIds.forEach { messageId ->
            if (isValidMessageId(messageId)) {
                val message = getSmsBodyById(this, messageId)
                if (message.toString().isNotBlank()) {
                    try {
                        val defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()

                        val subscriptionId =
                            SharedPreferencesHelper.getInt(
                                this,
                                Const.SIM_SLOT_NUMBER,
                                defaultSmsSubId
                            )
                        val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                        val parts = smsManager.divideMessage(message)
                        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

                        insertSmsSent(this, phoneNumber, message.toString())
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this,
                            getString(R.string.failed_to_send_sms),
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                }
            } else {
                val selectedImageUris = mutableListOf<Uri>()
                val regex = Regex("^\\d+")
                val messageIdForImage = regex.find(messageId)?.value ?: ""

                selectedImageUris.add("content://mms/part/$messageIdForImage".toUri())

                if (selectedImageUris.isNotEmpty()) {
                    insertMmsSent(this, phoneNumber, selectedImageUris)
                }
            }
        }
    }

    private fun isValidMessageId(messageId: String): Boolean {
        return !messageId.contains("(Images)")
    }

    private fun insertSmsSent(context: Context, phoneNumber: String, message: String) {
        val subscriptionId = SharedPreferencesHelper.getInt(
            this, Const.SIM_SLOT_NUMBER, SubscriptionManager.getDefaultSmsSubscriptionId()
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
            .appendQueryParameter("recipient", phoneNumber).build()
        context.contentResolver.query(uri, arrayOf("_id"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        throw Exception("Failed to get or create thread ID")
    }

    private fun filterContactsWithHeaders(query: String, withNumberSearch: Boolean = false) {
        val filtered = mutableListOf<ContactModel>()
        val groupedMap = linkedMapOf<String, MutableList<ContactModel.ContactItem>>()
        val lowerCaseQuery = query.trim().lowercase()

        if (lowerCaseQuery.isEmpty()) {
            rvAllContactListAdapter.updateData(allContactsList)
            binding.rvNoFavouriteContacts.visibility = View.GONE
            binding.rvAllContactsList.fadeIn()
            return
        }

        var currentHeaderTitle: String? = null

        allContactsList.forEach { item ->
            when (item) {
                is ContactModel.HeaderWithFirstItem -> {
                    currentHeaderTitle = item.title

                    val nameMatches =
                        item.firstContact.name?.lowercase()?.contains(lowerCaseQuery) == true
                    val phoneMatches =
                        item.firstContact.phoneNumbers?.contains(lowerCaseQuery)

                    if (nameMatches || phoneMatches == true) {
                        val headerTitle = currentHeaderTitle
                        val group = groupedMap.getOrPut(headerTitle) { mutableListOf() }
                        group.add(item.firstContact)
                    }
                }

                is ContactModel.ContactItem -> {
                    val nameMatches = item.name?.lowercase()?.contains(lowerCaseQuery) == true
                    val phoneMatches = item.phoneNumbers?.contains(lowerCaseQuery)

                    if (nameMatches || phoneMatches == true) {
                        val headerTitle = currentHeaderTitle ?: "#"
                        val group = groupedMap.getOrPut(headerTitle) { mutableListOf() }
                        group.add(item)
                    }
                }
            }
        }

        groupedMap.forEach { (headerTitle, contacts) ->
            val firstContact = contacts.first()
            filtered.add(
                ContactModel.HeaderWithFirstItem(
                    title = headerTitle,
                    firstContact = firstContact
                )
            )
            if (contacts.size > 1) {
                filtered.addAll(contacts.drop(1))
            }
        }

        if (filtered.isNotEmpty()) {
            binding.rvNoFavouriteContacts.visibility = View.GONE
            binding.rvAllContactsList.visibility = View.VISIBLE
            rvAllContactListAdapter.updateData(filtered)
        } else {
            binding.rvAllContactsList.visibility = View.GONE
            if (!withNumberSearch) {
                binding.rvNoFavouriteContacts.fadeIn()
            }
        }
    }

    override fun onItemRemoved(
        contactDetails: ContactModel.ContactItem,
        position: Int
    ) {
        forwardRecipientsList.remove(contactDetails)
        val selectedMobileList =
            SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MOBILE_LIST)
        if (selectedMobileList.contains(contactDetails.phoneNumbers)) {
            selectedMobileList.remove(contactDetails.phoneNumbers)
        }
        SharedPreferencesHelper.saveArrayList(
            this,
            Const.SELECTED_MOBILE_LIST,
            selectedMobileList
        )
        updateSelectedCount(selectedMobileList.size)
        rvAllContactListAdapter.specificClearAndUpdateView(contactDetails.phoneNumbers.toString())
    }

    override fun onRemoveSelectedMessage(message: String, position: Int) {
        val selectedMessagesIds =
            SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
        selectedMessagesIds.remove(message)
        SharedPreferencesHelper.saveArrayList(
            this,
            Const.SELECTED_MESSAGE_IDS,
            selectedMessagesIds
        )

        if (selectedMessagesIds.isEmpty()) {
            binding.rvSenderView.fadeOut()
        } else {
            binding.rvSenderView.fadeIn()
        }

        rvForwardMessageListAdapter.updateData(selectedMessagesIds)
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }

    private fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }
}