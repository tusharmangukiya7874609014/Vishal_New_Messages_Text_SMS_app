package com.texting.sms.messaging_app.activity

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.ChatUserAdapter
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityBlockedMessagesBinding
import com.texting.sms.messaging_app.databinding.DialogBlockNumberBinding
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.listener.OnClickMessagesFeature
import com.texting.sms.messaging_app.model.ChatUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedMessagesActivity : BaseActivity(), OnChatUserInterface, OnClickMessagesFeature {
    private lateinit var binding: ActivityBlockedMessagesBinding
    private lateinit var rvBlockMessageListAdapter: ChatUserAdapter
    private lateinit var blockMessageList: List<ChatUser>
    private var storeThreadIDList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_blocked_messages)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", -1L)
        initView()
        initClickListener()
    }

    private fun initView() {
        storeThreadIDList.clear()
        SharedPreferencesHelper.saveArrayList(this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList)

        binding.rvBlockedMessageList.layoutManager = LinearLayoutManager(this)
        rvBlockMessageListAdapter = ChatUserAdapter(
            mutableListOf(), storeThreadIDList, this, this, 0, false, this
        )
        binding.rvBlockedMessageList.adapter = rvBlockMessageListAdapter
        (binding.rvBlockedMessageList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        CoroutineScope(Dispatchers.IO).launch {
            blockMessageList = getBlockedChatUsers(this@BlockedMessagesActivity)

            withContext(Dispatchers.Main) {
                if (blockMessageList.isNotEmpty()) {
                    binding.paginationProgress.visibility = View.GONE
                    binding.rvNoMessageView.visibility = View.GONE
                    binding.rvBlockedMessageList.fadeIn()
                    rvBlockMessageListAdapter.updateData(blockMessageList)
                } else {
                    binding.paginationProgress.visibility = View.GONE
                    binding.rvBlockedMessageList.visibility = View.GONE
                    binding.rvNoMessageView.fadeIn()
                }
            }
        }
    }

    private fun getBlockedChatUsers(context: Context): List<ChatUser> {
        val blockedNumbers = getBlockedNumbers(context).map { normalizeNumber(it) }.toSet()
        if (blockedNumbers.isEmpty()) return emptyList()

        val chatMap = mutableMapOf<String, ChatUser>()

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        val cursor = context.contentResolver.query(
            uri, projection, null, null, "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            val idxThread = it.getColumnIndex(Telephony.Sms.THREAD_ID)
            val idxAddress = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
            val idxDate = it.getColumnIndex(Telephony.Sms.DATE)
            val idxRead = it.getColumnIndex(Telephony.Sms.READ)
            val idxSubId = it.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

            while (it.moveToNext()) {
                val threadId = it.getLong(idxThread)
                val rawAddress = it.getString(idxAddress) ?: continue

                if (!isPhoneNumber(rawAddress)) continue

                val normalized = normalizeNumber(rawAddress)
                if (normalized.isEmpty() || !blockedNumbers.contains(normalized)) continue

                val body = it.getString(idxBody) ?: ""
                val date = it.getLong(idxDate)
                val isRead = it.getInt(idxRead) == 1
                val simSlot = if (idxSubId != -1) it.getInt(idxSubId) else -1

                val existing = chatMap[normalized]
                if (existing == null || date > existing.timestamp) {
                    chatMap[normalized] = ChatUser(
                        threadId = threadId,
                        latestMessage = body,
                        timestamp = date,
                        address = rawAddress,
                        contactName = "",
                        photoUri = "",
                        unreadCount = if (!isRead) 1 else 0,
                        simSlot = simSlot
                    )
                } else if (!isRead) {
                    chatMap[normalized]?.unreadCount = (chatMap[normalized]?.unreadCount ?: 0) + 1
                }
            }
        }

        return chatMap.values.sortedByDescending { it.timestamp }
    }

    private fun isPhoneNumber(address: String): Boolean {
        return address.matches(Regex("^\\+?[0-9]{7,15}$"))
    }

    private fun normalizeNumber(number: String): String {
        return number.replace("[^0-9]".toRegex(), "")
            .let { if (it.length > 10) it.takeLast(10) else it }
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun getBlockedNumbers(context: Context): List<String> {
        val blockedNumbers = mutableListOf<String>()
        if (!isDefaultSmsApp(context)) {
            showToast(context.getString(R.string.app_is_not_default_sms_app))
            return blockedNumbers
        }

        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val index =
                    it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                while (it.moveToNext()) {
                    val number = it.getString(index)
                    if (!number.isNullOrBlank()) {
                        blockedNumbers.add(normalizeNumber(number))
                    }
                }
            }
        } catch (e: SecurityException) {
            showToast(
                context.getString(
                    R.string.permission_denied_or_app_is_not_eligible, e.message
                )
            )
        }

        return blockedNumbers
    }

    override fun chatUserClick(userChatDetails: ChatUser) {
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", userChatDetails.threadId)
        val intent = Intent(this, PersonalChatActivity::class.java)
        intent.putExtra(Const.THREAD_ID, userChatDetails.threadId)
        intent.putExtra(Const.SENDER_ID, userChatDetails.address)
        startActivity(intent)
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        } else {
            val lastThreadID = SharedPreferencesHelper.getLong(this, "CURRENT_THREAD_ID", -1L)
            if (lastThreadID != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedRecord =
                        getSmsChatUserByThreadId(this@BlockedMessagesActivity, lastThreadID)

                    withContext(Dispatchers.Main) {
                        if (updatedRecord != null) {
                            rvBlockMessageListAdapter.updateItemByThreadId(
                                lastThreadID, updatedRecord
                            )
                        } else {
                            val mutableList = blockMessageList.toMutableList()
                            mutableList.removeAll { it.threadId == lastThreadID }
                            blockMessageList = mutableList.toList()
                            rvBlockMessageListAdapter.updateData(blockMessageList)
                        }
                    }
                }
            }
        }
        super.onResume()
    }

    private fun getSmsChatUserByThreadId(
        context: Context, threadId: Long
    ): ChatUser? {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val cursor = context.contentResolver.query(
            uri, projection, selection, selectionArgs, "${Telephony.Sms.DATE} DESC"
        )

        var latestMessage: String? = null
        var latestTimestamp = 0L
        var rawAddress: String? = null
        var unreadCount = 0
        var simSlot = -1

        cursor?.use {
            val idxAddress = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val idxBody = it.getColumnIndex(Telephony.Sms.BODY)
            val idxDate = it.getColumnIndex(Telephony.Sms.DATE)
            val idxRead = it.getColumnIndex(Telephony.Sms.READ)

            while (it.moveToNext()) {
                val date = it.getLong(idxDate)
                val isRead = it.getInt(idxRead) == 1

                if (latestTimestamp == 0L) {
                    latestTimestamp = date
                    latestMessage = it.getString(idxBody) ?: ""
                    rawAddress = it.getString(idxAddress)
                    simSlot = try {
                        it.getInt(it.getColumnIndexOrThrow("sub_id"))
                    } catch (_: Exception) {
                        -1
                    }
                }

                if (!isRead) unreadCount++
            }
        }

        rawAddress ?: return null

        val normalized = normalizeNumber(rawAddress)
        val blockedNumbers = getBlockedNumbers(context).map { normalizeNumber(it) }.toSet()

        if (!blockedNumbers.contains(normalized)) {
            return null
        }

        return ChatUser(
            threadId = threadId,
            latestMessage = latestMessage ?: "",
            timestamp = latestTimestamp,
            address = rawAddress,
            contactName = "",
            photoUri = "",
            unreadCount = unreadCount,
            simSlot = simSlot
        )
    }

    override fun onClickOfMessageFeature(threadId: Long, type: String) {
        when (type) {
            "BLOCKED" -> {
                val contactInfo = getPhoneNumberOrAddressFromThreadId(
                    this@BlockedMessagesActivity, threadId
                )
                val isBlockedOrNot = try {
                    BlockedNumberContract.isBlocked(this, contactInfo)
                } catch (_: Exception) {
                    false
                }
                val selectedThreadID =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                selectedThreadID.add(threadId.toString())
                SharedPreferencesHelper.saveArrayList(
                    this, Const.SELECTED_MESSAGE_IDS, selectedThreadID
                )
                if (isBlockedOrNot) {
                    showUnblockConfirmationDialog()
                }
            }
        }
    }

    private fun showUnblockConfirmationDialog() {
        val dialog = Dialog(this)
        val dialogBlockOrUnblockBinding: DialogBlockNumberBinding =
            DialogBlockNumberBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBlockOrUnblockBinding.root)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)

            val metrics = resources.displayMetrics
            window.attributes = window.attributes.apply {
                width = (metrics.widthPixels * 0.9f).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        dialogBlockOrUnblockBinding.apply {
            rvBlockNumber.visibility = View.GONE
            txtStatement.visibility = View.VISIBLE
            txtTitle.text = resources.getString(R.string.unblock)
            txtStatement.text =
                getString(R.string.are_you_sure_you_want_to_unblock_this_conversations)
            btnYes.text = resources.getString(R.string.unblock)
        }

        dialogBlockOrUnblockBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBlockOrUnblockBinding.btnYes.setOnClickListener {
            dialog.dismiss()

            lifecycleScope.launch {
                val updatedList = withContext(Dispatchers.IO) {
                    val selectedThreadID = SharedPreferencesHelper.getArrayList(
                        this@BlockedMessagesActivity, Const.SELECTED_MESSAGE_IDS
                    )

                    val mutableList = blockMessageList.toMutableList()

                    selectedThreadID.forEach { threadId ->
                        val contactInfo = getPhoneNumberOrAddressFromThreadId(
                            this@BlockedMessagesActivity, threadId.toLong()
                        )

                        unblockNumber(
                            this@BlockedMessagesActivity, contactInfo.toString()
                        )

                        mutableList.removeAll { it.threadId == threadId.toLong() }
                    }

                    mutableList
                }

                blockMessageList = updatedList

                if (blockMessageList.isNotEmpty()) {
                    binding.rvNoMessageView.visibility = View.GONE
                    rvBlockMessageListAdapter.updateData(blockMessageList)
                } else {
                    binding.rvBlockedMessageList.visibility = View.GONE
                    binding.rvNoMessageView.fadeIn()
                }

                showToast(
                    getString(R.string.contact_has_been_unblocked_successfully)
                )
            }
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun getPhoneNumberOrAddressFromThreadId(context: Context, threadId: Long): String? {
        val smsUri = "content://sms".toUri()
        val projection = arrayOf("address")
        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "date DESC LIMIT 1"

        context.contentResolver.query(smsUri, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                    if (!address.isNullOrEmpty()) return address
                }
            }

        val canonicalUri = "content://mms-sms/canonical-addresses".toUri()
        context.contentResolver.query(canonicalUri, arrayOf("_id", "address"), null, null, null)
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                    if (id == threadId) {
                        val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                        if (!address.isNullOrEmpty()) return address
                    }
                }
            }

        return null
    }

    private fun unblockNumber(context: Context, number: String): Boolean {
        if (!isDefaultSmsApp(context)) {
            return false
        }

        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
        val selectionArgs = arrayOf(number)

        return try {
            val rowsDeleted = context.contentResolver.delete(uri, selection, selectionArgs)
            rowsDeleted > 0
        } catch (_: Exception) {
            false
        }
    }
}