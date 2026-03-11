package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.ArchivedChatAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityArchivedBinding
import com.texting.sms.messaging_app.databinding.DialogBlockNumberBinding
import com.texting.sms.messaging_app.databinding.DialogDeleteConversationBinding
import com.texting.sms.messaging_app.listener.CallbackHolder
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.listener.OnClickMessagesFeature
import com.texting.sms.messaging_app.model.ChatUser
import com.texting.sms.messaging_app.utils.getDrawableFromAttr
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.iterator

class ArchivedActivity : BaseActivity(), OnChatUserInterface, OnClickMessagesFeature {
    private lateinit var binding: ActivityArchivedBinding
    private lateinit var rvArchivedListAdapter: ArchivedChatAdapter
    private lateinit var archivedMessageList: List<ChatUser>
    private var storeThreadIDList = ArrayList<String>()
    private var chatSMSSelected = 0
    private lateinit var editContactLauncher: ActivityResultLauncher<Intent>
    private var editContactThreadID = 0L
    private var isMultiSelectionEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_archived)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", -1L)
        runAdsCampion()
        initView()
        initClickListener()
        receiverSMSOrMMS()
        editContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    getArchivedSMSResponse()
                    val data = result.data
                    val contactUri = data?.data
                    contactUri?.let {
                        val contactName = getContactNameFromUri(this, it)
                        rvArchivedListAdapter.updateAfterAddToContacts(
                            editContactThreadID,
                            contactName
                        )
                    }
                }
            }
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@ArchivedActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ArchivedActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ArchivedActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ArchivedActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@ArchivedActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("ArchivedActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ArchivedActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ArchivedActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ArchivedActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val bannerAdsType = getAdsPageResponse.optString("bannerAdsType").ifEmpty {
                "adaptive"
            } ?: "adaptive"

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isCurrentPageNativeAdsEnabled && !isCurrentPageBannerAdsEnabled) {
                    runNativeAds(
                        nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
                    )
                } else if (isCurrentPageBannerAdsEnabled && !isCurrentPageNativeAdsEnabled) {
                    runBannerAds(
                        bannerAdsId = currentPageBannerAdsID, bannerAdsType = bannerAdsType
                    )
                }

                if (isAppInterstitialAdsEnabled) {
                    InterstitialAdHelper.apply {
                        loadAd(this@ArchivedActivity)
                    }
                }
            }
        }
    }

    private fun runNativeAds(
        nativeAdsType: String, nativeAdsId: String
    ) {
        binding.nativeAdContainer.visibility = View.VISIBLE
        NativeAdHelper.loadNativeAd(
            this, binding.nativeAdContainer, nativeAdsType, nativeAdsId = nativeAdsId
        )
    }

    private fun runBannerAds(
        bannerAdsId: String, bannerAdsType: String
    ) {
        val bannerType = when (bannerAdsType) {
            "medium_rectangle" -> {
                BannerType.MEDIUM_RECTANGLE
            }

            else -> {
                BannerType.ADAPTIVE
            }
        }

        when (bannerAdsType) {
            "medium_rectangle" -> {
                binding.bannerAdContainer.iAdaptiveShimmer.root.visibility = View.GONE
                binding.bannerAdContainer.iMediumRectangleShimmer.root.visibility = View.VISIBLE
            }

            else -> {
                binding.bannerAdContainer.iMediumRectangleShimmer.root.visibility = View.GONE
                binding.bannerAdContainer.iAdaptiveShimmer.root.visibility = View.VISIBLE
            }
        }
        binding.bannerAdContainer.root.visibility = View.VISIBLE
        BannerAdHelper.loadBannerAd(
            this,
            binding.bannerAdContainer.bannerAdView,
            binding.bannerAdContainer.shimmerViewContainer,
            bannerType = bannerType,
            view = binding.bannerAdContainer.view,
            bannerAdsId = bannerAdsId
        )
    }

    private fun getContactNameFromUri(context: Context, uri: Uri): String? {
        val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
            }
        }
        return null
    }

    fun updateSelectedCount(selectedCount: Int) {
        chatSMSSelected = selectedCount
        if (selectedCount >= 0) {
            binding.rvTopHeaderView.visibility = View.INVISIBLE
            binding.iSelectionHeader.root.fadeIn()
            val finalMessage = getString(R.string.selected, selectedCount.toString())
            binding.iSelectionHeader.txtSelectedCount.text = finalMessage

            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            var pinnedCount = 0
            var unpinnedCount = 0

            selectedThreadID.forEach { threadId ->
                val isPinned = SharedPreferencesHelper.isPinned(this, threadId.toLong())
                if (isPinned) pinnedCount++ else unpinnedCount++
            }

            when {
                pinnedCount > unpinnedCount -> {
                    binding.iSelectionHeader.ivPin.tag = "Unpin"
                    binding.iSelectionHeader.ivPin.setImageDrawable(getDrawableFromAttr(R.attr.unPinIcon))
                }

                unpinnedCount > pinnedCount -> {
                    binding.iSelectionHeader.ivPin.tag = "Pin"
                    binding.iSelectionHeader.ivPin.setImageDrawable(getDrawableFromAttr(R.attr.pinIcon))
                }

                else -> {
                    binding.iSelectionHeader.ivPin.tag = "Pin"
                    binding.iSelectionHeader.ivPin.setImageDrawable(getDrawableFromAttr(R.attr.pinIcon))
                }
            }
        } else {
            binding.ivMultiSelection.imageTintList = null
            isMultiSelectionEnable = false
            rvArchivedListAdapter.clearAndUpdateView()
            binding.iSelectionHeader.root.visibility = View.GONE
            binding.rvTopHeaderView.visibility = View.VISIBLE
        }
    }

    private fun initView() {
        binding.iSelectionHeader.ivArchived.setImageDrawable(getDrawableFromAttr(R.attr.unarchivedIcon))
        storeThreadIDList.clear()
        SharedPreferencesHelper.saveArrayList(
            this,
            Const.SELECTED_MESSAGE_IDS,
            storeThreadIDList
        )
        val layoutManagerMessage: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvArchivedMessageList.setLayoutManager(layoutManagerMessage)
        rvArchivedListAdapter = ArchivedChatAdapter(
            mutableListOf(),
            storeThreadIDList,
            this,
            this,
            getActiveSimCount(this),
            this
        )
        binding.rvArchivedMessageList.adapter = rvArchivedListAdapter
        (binding.rvArchivedMessageList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
    }

    private fun getActiveSimCount(context: Context): Int {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return 0
        }

        val subscriptionManager =
            context.getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val activeSubs = subscriptionManager.activeSubscriptionInfoList
        return if (activeSubs != null && activeSubs.size == 2) {
            2
        } else {
            0
        }
    }

    private fun receiverSMSOrMMS() {
        contentResolver.registerContentObserver(
            "content://sms".toUri(),
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    CoroutineScope(Dispatchers.IO).launch {
                        val archivedThreadIdMap =
                            SharedPreferencesHelper.getArchivedThreadIds(this@ArchivedActivity)
                        val archivedThreadIds: List<Long> =
                            archivedThreadIdMap.mapNotNull { it.toLongOrNull() }
                        val archivedSmsChatList =
                            getThreadsByIds(this@ArchivedActivity, archivedThreadIds)

                        withContext(Dispatchers.Main) {
                            afterApplyFilterUpdateMessages(archivedSmsChatList)
                        }
                    }
                }
            }
        )
    }

    private fun getThreadsByIds(
        context: Context,
        threadIds: List<Long>
    ): List<ChatUser> {
        if (threadIds.isEmpty()) return emptyList()

        val smsThreads = mutableListOf<ChatUser>()
        val contentResolver = context.contentResolver

        val smsUri = "content://sms".toUri()
        val projection = arrayOf("thread_id", "address", "body", "date", "read", "sub_id", "type")

        val selection = "thread_id IN (${threadIds.joinToString(",") { "?" }})"
        val selectionArgs = threadIds.map { it.toString() }.toTypedArray()

        val cursor = contentResolver.query(
            smsUri,
            projection,
            selection,
            selectionArgs,
            "date DESC"
        )

        val threadMap = mutableMapOf<Long, MutableList<SmsPart>>()
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
                list.add(SmsPart(address, body, date, isRead, simSlot, type))
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
                } else break
            }

            val latestMessage = sortedMessages.first()
            latestMergedMessage = buffer.toString()
            val previewText = if (latestMessage.type == 2) {
                context.getString(R.string.you, latestMergedMessage)
            } else {
                latestMergedMessage
            }

            val unreadCount = unreadCounter[threadId] ?: 0
            val latestSimSlot = sortedMessages.first().simSlot

            smsThreads.add(
                ChatUser(
                    threadId = threadId,
                    latestMessage = previewText,
                    timestamp = latestTimestamp,
                    address = sortedMessages.first().address,
                    contactName = "",
                    photoUri = "",
                    unreadCount = unreadCount,
                    simSlot = latestSimSlot
                )
            )
        }

        return smsThreads.sortedByDescending { it.timestamp }
    }

    data class SmsPart(
        val address: String,
        val body: String,
        val date: Long,
        val isRead: Boolean,
        val simSlot: Int,
        val type: Int
    )

    private fun restoreMessageFromArchive(messageId: ArrayList<String>) {
        messageId.forEach { threadId ->
            SharedPreferencesHelper.saveArchivedThread(this, threadId.toLong())
        }
        getArchivedSMSResponse()
        CallbackHolder.listener?.onUpdateTheRecyclerView(true)
    }

    private fun initClickListener() {
        binding.iSelectionHeader.ivRemoveSelection.setOnClickListener {
            updateSelectedCount(-1)
            storeThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this,
                Const.SELECTED_MESSAGE_IDS,
                storeThreadIDList
            )
            rvArchivedListAdapter.clearAndUpdateView()
        }

        binding.iSelectionHeader.ivArchived.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                selectedThreadID.forEach { threadId ->
                    SharedPreferencesHelper.removeArchivedThread(this, threadId.toLong())
                    val mutableList = archivedMessageList.toMutableList()
                    mutableList.removeAll { it.threadId == threadId.toLong() }

                    archivedMessageList = mutableList.toList()
                    rvArchivedListAdapter.removeByThreadId(threadId.toLong())
                }

                CallbackHolder.listener?.onUpdateTheRecyclerView(true)
                clearSelectionViewOrUpdate()

                val snackbar = Snackbar.make(
                    binding.rvArchivedMessageList,
                    getString(R.string.conversation_unarchived, selectedThreadID.size),
                    Snackbar.LENGTH_LONG
                )
                    .setAction(getString(R.string.undo)) {
                        restoreMessageFromArchive(selectedThreadID)
                    }

                snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.app_theme_color))
                val snackbarView = snackbar.view
                val params = snackbarView.layoutParams as FrameLayout.LayoutParams
                params.setMargins(
                    params.leftMargin,
                    params.topMargin,
                    params.rightMargin,
                    params.bottomMargin + resources.getDimensionPixelSize(R.dimen.snackbar_bottom_margin)
                )
                snackbarView.layoutParams = params
                snackbar.show()
            } else {
                showToast(getString(R.string.first_select_message_atleast_one_or_more))
            }
        }

        binding.iSelectionHeader.ivPrivateChats.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                selectedThreadID.forEach { threadId ->
                    SharedPreferencesHelper.savePrivateThread(this, threadId.toLong())
                    val mutableList = archivedMessageList.toMutableList()
                    mutableList.removeAll { it.threadId == threadId.toLong() }

                    archivedMessageList = mutableList.toList()
                    rvArchivedListAdapter.removeByThreadId(threadId.toLong())
                }
                clearSelectionViewOrUpdate()
            } else {
                showToast(getString(R.string.first_select_message_atleast_one_or_more))
            }
        }

        binding.iSelectionHeader.ivPin.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                selectedThreadID.forEach { threadId ->
                    if (binding.iSelectionHeader.ivPin.tag.toString().contentEquals("Pin")) {
                        SharedPreferencesHelper.setPinned(this, threadId.toLong(), true)
                    } else {
                        SharedPreferencesHelper.setPinned(this, threadId.toLong(), false)
                    }
                }
                clearSelectionViewOrUpdate()
            } else {
                showToast(getString(R.string.first_select_message_atleast_one_or_more))
            }
        }

        binding.iSelectionHeader.ivMore.setOnClickListener {
            if (binding.iSelectionHeader.cvMoreOptionsDialog.isVisible) {
                binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            } else {
                if (chatSMSSelected > 1) {
                    binding.iSelectionHeader.txtAddToContacts.visibility = View.GONE
                    binding.iSelectionHeader.txtDialNumber.visibility = View.GONE
                    binding.iSelectionHeader.txtCopyToClipboard.visibility = View.GONE
                } else {
                    binding.iSelectionHeader.txtAddToContacts.visibility = View.VISIBLE
                    binding.iSelectionHeader.txtDialNumber.visibility = View.VISIBLE
                    binding.iSelectionHeader.txtCopyToClipboard.visibility = View.VISIBLE
                }
                val selectedThreadID =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                if (selectedThreadID.isNotEmpty()) {
                    var blocked = 0
                    var unBlocked = 0
                    selectedThreadID.forEach { threadId ->
                        val contactInfo =
                            getPhoneNumberOrAddressFromThreadId(
                                this@ArchivedActivity,
                                threadId.toLong()
                            )
                        if (SharedPreferencesHelper.isBlocked(
                                this@ArchivedActivity,
                                contactInfo.toString()
                            )
                        ) {
                            blocked++
                        } else {
                            unBlocked++
                        }
                    }
                    if (blocked > unBlocked) {
                        binding.iSelectionHeader.txtBlock.text =
                            resources.getString(R.string.unblock)
                    } else {
                        binding.iSelectionHeader.txtBlock.text = resources.getString(R.string.block)
                    }
                    binding.iSelectionHeader.cvMoreOptionsDialog.fadeIn()
                } else {
                    showToast(getString(R.string.first_select_message_atleast_one_or_more))
                }
            }
        }

        binding.iSelectionHeader.txtDelete.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            showDeleteMultipleConversationDialog()
        }

        binding.iSelectionHeader.txtMarkAsRead.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                val contentValues = ContentValues().apply {
                    put("read", 1)
                }

                val uri = "content://sms/inbox".toUri()
                val selection = "thread_id = ? AND read = 0"
                val selectionArgs = arrayOf(threadId)

                try {
                    contentResolver.update(uri, contentValues, selection, selectionArgs)
                    rvArchivedListAdapter.updateMarkAsRead(threadId.toLong())
                } catch (e: Exception) {
                    Log.e("ABCD", "Failed to mark messages as read", e)
                }
            }
            clearSelectionViewOrUpdate()
        }

        binding.iSelectionHeader.txtDialNumber.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                val phoneNumber =
                    getPhoneNumberFromThreadId(this@ArchivedActivity, threadId.toLong())
                val sanitizedNumber = phoneNumber?.replace(Regex("[^\\d+]"), "")

                if (sanitizedNumber?.isNotEmpty() == true) {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:$phoneNumber".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(dialIntent)
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to start dialer", e)
                    }
                }
            }
            clearSelectionViewOrUpdate()
        }

        binding.iSelectionHeader.txtAddToContacts.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                editContactThreadID = threadId.toLong()
                openContactEditorFromThreadId(this@ArchivedActivity, threadId.toLong())
            }
            clearSelectionViewOrUpdate()
        }

        binding.iSelectionHeader.txtBlock.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            if (binding.iSelectionHeader.txtBlock.text.contentEquals(resources.getString(R.string.block))) {
                showBlockConfirmationDialog()
            } else {
                showUnblockConfirmationDialog()
            }
        }

        binding.iSelectionHeader.txtCopyToClipboard.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                val address = getAddressFromThreadId(this@ArchivedActivity, threadId.toLong())
                copyToClipboard(this@ArchivedActivity, address.toString())
            }
            clearSelectionViewOrUpdate()
        }

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvMultiSelection.setOnClickListener {
            if (isMultiSelectionEnable) {
                binding.ivMultiSelection.imageTintList = null
                isMultiSelectionEnable = false
                rvArchivedListAdapter.updateSelectionView(false)
                updateSelectedCount(-1)
            } else {
                isMultiSelectionEnable = true
                binding.ivMultiSelection.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        R.color.app_theme_color
                    )
                )
                rvArchivedListAdapter.updateSelectionView(true)
                updateSelectedCount(0)
            }
        }
    }

    private fun copyToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Address", code)
        clipboard.setPrimaryClip(clip)
    }

    private fun getAddressFromThreadId(context: Context, threadId: Long): String? {
        val uri = "content://sms".toUri()
        val projection = arrayOf("address")
        val selection = "thread_id = ?"
        val selectionArgs = arrayOf(threadId.toString())
        val sortOrder = "date DESC LIMIT 1"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow("address"))
                }
            }
        return null
    }

    private fun clearSelectionViewOrUpdate() {
        afterApplyFilterUpdateMessages(archivedMessageList)
        updateSelectedCount(-1)
        storeThreadIDList.clear()
        SharedPreferencesHelper.saveArrayList(
            this,
            Const.SELECTED_MESSAGE_IDS,
            storeThreadIDList
        )
        rvArchivedListAdapter.clearAndUpdateView()
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

    private fun showDeleteMultipleConversationDialog() {
        val dialog = Dialog(this)
        val deleteConversationDialogBinding: DialogDeleteConversationBinding =
            DialogDeleteConversationBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(deleteConversationDialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        deleteConversationDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        deleteConversationDialogBinding.btnYes.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

            selectedThreadID.forEach { threadId ->
                if (deleteSmsConversation(this@ArchivedActivity, threadId.toLong())) {
                    SharedPreferencesHelper.addDeletedThreadId(
                        this@ArchivedActivity,
                        threadId.toLong()
                    )
                    val mutableList = archivedMessageList.toMutableList()
                    mutableList.removeAll { it.threadId == threadId.toLong() }

                    archivedMessageList = mutableList.toList()
                    rvArchivedListAdapter.removeByThreadId(threadId.toLong())

                    if (SharedPreferencesHelper.isPinned(
                            this@ArchivedActivity,
                            threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.setPinned(this, threadId.toLong(), false)
                    }
                    if (SharedPreferencesHelper.isThreadArchived(
                            this@ArchivedActivity,
                            threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.removeArchivedThread(
                            this@ArchivedActivity,
                            threadId.toLong()
                        )
                    }
                    if (SharedPreferencesHelper.isThreadPrivate(
                            this@ArchivedActivity,
                            threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.removePrivateThread(
                            this@ArchivedActivity,
                            threadId.toLong()
                        )
                    }
                }
            }

            clearSelectionViewOrUpdate()
            dialog.dismiss()
        }
        dialog.setCancelable(true)
        if (!isFinishing && !isDestroyed) {
            val window = dialog.window
            val params = window?.attributes
            val displayMetrics = resources.displayMetrics
            params?.width = (displayMetrics.widthPixels * 0.9).toInt()
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            dialog.show()
        }
    }

    private fun deleteSmsConversation(context: Context, threadId: Long): Boolean {
        if (!isDefaultSmsApp(context)) {
            showToast(getString(R.string.app_must_be_default_sms_app_to_delete_messages))
            return false
        }

        return try {
            val uri = "content://mms-sms/conversations/$threadId".toUri()
            val rowsDeleted = context.contentResolver.delete(uri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun openContactEditorFromThreadId(context: Context, threadId: Long) {
        val address = getAddressFromThreadId(context, threadId)
        if (address != null) {
            val contactId = getContactIdFromPhoneNumber(context, address)
            if (contactId != null) {
                val uri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactId.toLong()
                )
                val intent = Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    putExtra("finishActivityOnSaveCompleted", true)
                }
                editContactLauncher.launch(intent)
            } else {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE
                    putExtra(ContactsContract.Intents.Insert.PHONE, address)
                    putExtra("finishActivityOnSaveCompleted", true)
                }
                startActivity(intent)
            }
        } else {
            showToast(getString(R.string.address_not_found_from_threadid))
        }
    }

    private fun getContactIdFromPhoneNumber(context: Context, phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID))
            }
        }
        return null
    }

    private fun showBlockConfirmationDialog() {
        val dialog = Dialog(this)
        val dialogBlockOrUnblockBinding: DialogBlockNumberBinding =
            DialogBlockNumberBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBlockOrUnblockBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        dialogBlockOrUnblockBinding.rvBlockNumber.visibility = View.GONE
        dialogBlockOrUnblockBinding.txtStatement.visibility = View.VISIBLE
        val titleTxt = resources.getString(R.string.block)
        val subTitleTxt = getString(R.string.are_you_sure_you_want_to_block_this_conversations)
        dialogBlockOrUnblockBinding.txtTitle.text = titleTxt
        dialogBlockOrUnblockBinding.txtStatement.text = subTitleTxt
        dialogBlockOrUnblockBinding.btnYes.text = resources.getString(R.string.block)

        dialogBlockOrUnblockBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBlockOrUnblockBinding.btnYes.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                val contactInfo =
                    getPhoneNumberOrAddressFromThreadId(this@ArchivedActivity, threadId.toLong())
                if (blockNumber(this@ArchivedActivity, contactInfo.toString())) {
                    SharedPreferencesHelper.addToBlockList(
                        this@ArchivedActivity,
                        contactInfo.toString()
                    )
                }
            }
            showToast(resources.getString(R.string.contact_has_been_blocked_successfully))
            rvArchivedListAdapter.updateBlockContacts()
            clearSelectionViewOrUpdate()
            dialog.dismiss()
        }
        if (!isFinishing && !isDestroyed) {
            val window = dialog.window
            val params = window?.attributes
            val displayMetrics = resources.displayMetrics
            params?.width = (displayMetrics.widthPixels * 0.9).toInt()
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            dialog.show()
        }
    }

    private fun showUnblockConfirmationDialog() {
        val dialog = Dialog(this)
        val dialogBlockOrUnblockBinding: DialogBlockNumberBinding =
            DialogBlockNumberBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBlockOrUnblockBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        dialogBlockOrUnblockBinding.rvBlockNumber.visibility = View.GONE
        dialogBlockOrUnblockBinding.txtStatement.visibility = View.VISIBLE
        val titleTxt = resources.getString(R.string.unblock)
        val subTitleTxt = getString(R.string.are_you_sure_you_want_to_unblock_this_conversations)
        dialogBlockOrUnblockBinding.txtTitle.text = titleTxt
        dialogBlockOrUnblockBinding.txtStatement.text = subTitleTxt
        dialogBlockOrUnblockBinding.btnYes.text = resources.getString(R.string.unblock)

        dialogBlockOrUnblockBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBlockOrUnblockBinding.btnYes.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                val contactInfo =
                    getPhoneNumberOrAddressFromThreadId(this@ArchivedActivity, threadId.toLong())
                unblockNumber(this@ArchivedActivity, contactInfo.toString())
            }
            showToast(resources.getString(R.string.contact_has_been_unblocked_successfully))
            rvArchivedListAdapter.updateBlockContacts()
            clearSelectionViewOrUpdate()
            dialog.dismiss()
        }
        if (!isFinishing && !isDestroyed) {
            val window = dialog.window
            val params = window?.attributes
            val displayMetrics = resources.displayMetrics
            params?.width = (displayMetrics.widthPixels * 0.9).toInt()
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            dialog.show()
        }
    }

    private fun blockNumber(context: Context, number: String): Boolean {
        if (!isDefaultSmsApp(context)) {
            return false
        }

        return try {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            val uri = context.contentResolver.insert(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                values
            )
            uri != null
        } catch (e: Exception) {
            Log.e("ABCD", "${e.localizedMessage}")
            false
        }
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

    override fun chatUserClick(userChatDetails: ChatUser) {
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", userChatDetails.threadId)
        if (SharedPreferencesHelper.getBoolean(
                this, Const.IS_ADS_ENABLED, false
            ) && SharedPreferencesHelper.getBoolean(
                this, Const.IS_INTERSTITIAL_ENABLED, false
            )
        ) {
            InterstitialAdHelper.apply {
                showAd(this@ArchivedActivity) {
                    val intent = Intent(this@ArchivedActivity, PersonalChatActivity::class.java)
                    intent.putExtra("THREAD_ID", userChatDetails.threadId)
                    intent.putExtra("SENDER_ID", userChatDetails.address)
                    startActivity(intent)
                }
            }
        } else {
            val intent = Intent(this, PersonalChatActivity::class.java)
            intent.putExtra("THREAD_ID", userChatDetails.threadId)
            intent.putExtra("SENDER_ID", userChatDetails.address)
            startActivity(intent)
        }
    }

    private fun getArchivedSMSResponse() {
        CoroutineScope(Dispatchers.IO).launch {
            val archivedThreadIdMap =
                SharedPreferencesHelper.getArchivedThreadIds(this@ArchivedActivity)
            val archivedThreadIds: List<Long> = archivedThreadIdMap.mapNotNull { it.toLongOrNull() }
            val archivedSmsChatList = getThreadsByIds(this@ArchivedActivity, archivedThreadIds)

            withContext(Dispatchers.Main) {
                afterApplyFilterUpdateMessages(archivedSmsChatList)
            }
        }
    }

    override fun onResume() {
        if (isDefaultSmsApp(this)) {
            val lastThreadID = SharedPreferencesHelper.getLong(this, "CURRENT_THREAD_ID", -1L)
            if (lastThreadID != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedRecord =
                        getSmsChatUserByThreadId(this@ArchivedActivity, lastThreadID)

                    withContext(Dispatchers.Main) {
                        updatedRecord?.let { record ->
                            rvArchivedListAdapter.updateItemByThreadId(lastThreadID, record)
                        }
                    }
                }
            }
            getArchivedSMSResponse()
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }

    private fun getSmsChatUserByThreadId(
        context: Context,
        threadId: Long
    ): ChatUser? {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID,
            Telephony.Sms.TYPE
        )

        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
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
            val idxType = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val date = it.getLong(idxDate)
                val isRead = it.getInt(idxRead) == 1
                val type = if (idxType >= 0) it.getInt(idxType) else -1

                if (latestTimestamp == 0L) {
                    latestTimestamp = date
                    val body = it.getString(idxBody) ?: ""

                    latestMessage = if (type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                        getString(R.string.you, body)
                    } else {
                        body
                    }
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
        val isPinned = SharedPreferencesHelper.isPinned(this, threadId)

        return ChatUser(
            threadId = threadId,
            latestMessage = latestMessage ?: "",
            timestamp = latestTimestamp,
            address = rawAddress,
            contactName = "",
            photoUri = "",
            isPinned = isPinned,
            unreadCount = unreadCount,
            simSlot = simSlot
        )
    }

    private fun afterApplyFilterUpdateMessages(chatList: List<ChatUser>) {
        archivedMessageList = chatList
        val pinnedMessageList = archivedMessageList.map {
            it.copy(
                isPinned = SharedPreferencesHelper.isPinned(
                    this@ArchivedActivity,
                    it.threadId
                )
            )
        }.sortedWith(
            compareByDescending<ChatUser> { it.isPinned }.thenByDescending { it.timestamp }
        )

        if (pinnedMessageList.isEmpty()) {
            binding.rvArchivedMessageList.visibility = View.GONE
            binding.paginationProgress.fadeOut()
            binding.rvNoMessageView.fadeIn()
        } else {
            binding.rvNoMessageView.visibility = View.GONE
            binding.rvArchivedMessageList.fadeIn()
            rvArchivedListAdapter.updateData(pinnedMessageList)
            binding.paginationProgress.fadeOut()
            binding.rvArchivedMessageList.alpha = 1f
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val viewRect = Rect()
        binding.iSelectionHeader.cvMoreOptionsDialog.getGlobalVisibleRect(viewRect)
        if (binding.iSelectionHeader.cvMoreOptionsDialog.isVisible && !viewRect.contains(
                ev.rawX.toInt(),
                ev.rawY.toInt()
            )
        ) {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onClickOfMessageFeature(
        threadId: Long,
        type: String
    ) {
        when (type) {
            "PIN" -> {
                if (SharedPreferencesHelper.isPinned(this, threadId = threadId)) {
                    SharedPreferencesHelper.setPinned(this, threadId, false)
                } else {
                    SharedPreferencesHelper.setPinned(this, threadId, true)
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    clearSelectionViewOrUpdate()
                }, 100)
            }

            "MARK_AS_READ" -> {
                rvArchivedListAdapter.updateMarkAsRead(threadId)
                val contentValues = ContentValues().apply {
                    put("read", 1)
                }

                val uri = "content://sms/inbox".toUri()
                val selection = "thread_id = ? AND read = 0"
                val selectionArgs = arrayOf(threadId.toString())

                try {
                    contentResolver.update(uri, contentValues, selection, selectionArgs)
                } catch (e: Exception) {
                    Log.e("ABCD", "Failed to mark messages as read", e)
                }
                getSmsChatUserByThreadId(this, threadId = threadId)
            }

            "ADD_TO_PRIVATE" -> {
                SharedPreferencesHelper.savePrivateThread(this, threadId)
                val mutableList = archivedMessageList.toMutableList()
                mutableList.removeAll { it.threadId == threadId }

                archivedMessageList = mutableList.toList()
                rvArchivedListAdapter.removeByThreadId(threadId)

                Handler(Looper.getMainLooper()).postDelayed({
                    clearSelectionViewOrUpdate()
                }, 100)
            }

            "BLOCKED" -> {
                val contactInfo =
                    getPhoneNumberOrAddressFromThreadId(
                        this@ArchivedActivity,
                        threadId
                    )
                val isBlockedOrNot = SharedPreferencesHelper.isBlocked(this, contactInfo.toString())
                val selectedThreadID =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                selectedThreadID.add(threadId.toString())
                SharedPreferencesHelper.saveArrayList(
                    this,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadID
                )
                if (isBlockedOrNot) {
                    showUnblockConfirmationDialog()
                } else {
                    showBlockConfirmationDialog()
                }
            }

            "DELETE" -> {
                val selectedThreadID =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                selectedThreadID.add(threadId.toString())
                SharedPreferencesHelper.saveArrayList(
                    this,
                    Const.SELECTED_MESSAGE_IDS,
                    selectedThreadID
                )
                showDeleteMultipleConversationDialog()
            }
        }
    }
}