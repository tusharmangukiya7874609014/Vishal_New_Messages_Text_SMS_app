package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Dialog
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
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityPrivateChatListBinding
import com.texting.sms.messaging_app.databinding.DialogBlockNumberBinding
import com.texting.sms.messaging_app.databinding.DialogDeleteConversationBinding
import com.texting.sms.messaging_app.listener.CallbackHolder
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.listener.OnClickMessagesFeature
import com.texting.sms.messaging_app.model.ChatUser
import com.texting.sms.messaging_app.utils.getDrawableFromAttr
import com.texting.sms.messaging_app.adapter.PrivateChatsAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.iterator

class PrivateChatListActivity : BaseActivity(), OnChatUserInterface, OnClickMessagesFeature {
    private lateinit var binding: ActivityPrivateChatListBinding
    private lateinit var privateMessageList: List<ChatUser>
    private lateinit var rvPrivateListAdapter: PrivateChatsAdapter
    private var storeThreadIDList = ArrayList<String>()
    private var isMultiSelectionEnable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_private_chat_list)
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
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@PrivateChatListActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@PrivateChatListActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@PrivateChatListActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@PrivateChatListActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@PrivateChatListActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("PrivateChatListActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@PrivateChatListActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@PrivateChatListActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@PrivateChatListActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        loadAd(this@PrivateChatListActivity)
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

    private fun initView() {
        storeThreadIDList.clear()

        val layoutManagerMessage: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvArchivedMessageList.setLayoutManager(layoutManagerMessage)
        rvPrivateListAdapter = PrivateChatsAdapter(
            mutableListOf(),
            storeThreadIDList,
            this, this,
            getActiveSimCount(this),
            this
        )
        binding.rvArchivedMessageList.adapter = rvPrivateListAdapter
        (binding.rvArchivedMessageList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
    }

    private fun getActiveSimCount(context: Context): Int {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_NUMBERS
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

    private fun initClickListener() {
        binding.rvMultiSelection.setOnClickListener {
            if (isMultiSelectionEnable) {
                binding.ivMultiSelection.imageTintList = null
                isMultiSelectionEnable = false
                rvPrivateListAdapter.updateSelectionView(false)
                updateSelectedCount(-1)
            } else {
                isMultiSelectionEnable = true
                binding.ivMultiSelection.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        R.color.app_theme_color
                    )
                )
                rvPrivateListAdapter.updateSelectionView(true)
                updateSelectedCount(0)
            }
        }

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivAddIntoThePrivateChat.setOnClickListener {
            startActivity(Intent(this, AllConversationsActivity::class.java))
        }

        binding.ivSetting.setOnClickListener {
            startActivity(Intent(this, PrivateChatSettingsActivity::class.java))
        }

        binding.iSelectionHeader.ivRemoveSelection.setOnClickListener {
            updateSelectedCount(-1)
            storeThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
            )
            rvPrivateListAdapter.clearAndUpdateView()
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

        binding.iSelectionHeader.ivDeleteChat.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
                showDeleteMultipleConversationDialog()
            } else {
                showToast(getString(R.string.first_select_message_atleast_one_or_more))
            }
        }

        binding.iSelectionHeader.ivMore.setOnClickListener {
            if (binding.iSelectionHeader.cvMoreOptionsDialog.isVisible) {
                binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            } else {
                val selectedThreadID =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                if (selectedThreadID.isNotEmpty()) {
                    var blocked = 0
                    var unBlocked = 0
                    selectedThreadID.forEach { threadId ->
                        val contactInfo = getPhoneNumberOrAddressFromThreadId(
                            this@PrivateChatListActivity, threadId.toLong()
                        )
                        if (SharedPreferencesHelper.isBlocked(
                                this@PrivateChatListActivity, contactInfo.toString()
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

        binding.iSelectionHeader.txtBlock.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            if (binding.iSelectionHeader.txtBlock.text.contentEquals(resources.getString(R.string.block))) {
                showBlockConfirmationDialog()
            } else {
                showUnblockConfirmationDialog()
            }
        }

        binding.iSelectionHeader.ivUnlockChat.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                selectedThreadID.forEach { threadId ->
                    SharedPreferencesHelper.removePrivateThread(this, threadId.toLong())
                    val mutableList = privateMessageList.toMutableList()
                    mutableList.removeAll { it.threadId == threadId.toLong() }

                    privateMessageList = mutableList.toList()
                    rvPrivateListAdapter.removeByThreadId(threadId.toLong())
                }
                CallbackHolder.listener?.onUpdateTheRecyclerView(true)
                clearSelectionViewOrUpdate()
            } else {
                showToast(getString(R.string.first_select_message_atleast_one_or_more))
            }
        }

        binding.iSelectionHeader.txtMarkAsRead.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                rvPrivateListAdapter.updateMarkAsRead(threadId.toLong())
                val contentValues = ContentValues().apply {
                    put("read", 1)
                }

                val uri = "content://sms/inbox".toUri()
                val selection = "thread_id = ? AND read = 0"
                val selectionArgs = arrayOf(threadId)

                try {
                    contentResolver.update(uri, contentValues, selection, selectionArgs)
                } catch (e: Exception) {
                    Log.e("ABCD", "Failed to mark messages as read", e)
                }
            }
            clearSelectionViewOrUpdate()
        }
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
                val contactInfo = getPhoneNumberOrAddressFromThreadId(
                    this@PrivateChatListActivity, threadId.toLong()
                )
                if (blockNumber(this@PrivateChatListActivity, contactInfo.toString())) {
                    SharedPreferencesHelper.addToBlockList(
                        this@PrivateChatListActivity, contactInfo.toString()
                    )
                }
            }
            showToast(resources.getString(R.string.contact_has_been_blocked_successfully))
            rvPrivateListAdapter.updateBlockContacts()
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
                val contactInfo = getPhoneNumberOrAddressFromThreadId(
                    this@PrivateChatListActivity, threadId.toLong()
                )
                unblockNumber(this@PrivateChatListActivity, contactInfo.toString())
            }
            showToast(resources.getString(R.string.contact_has_been_unblocked_successfully))
            rvPrivateListAdapter.updateBlockContacts()
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
                BlockedNumberContract.BlockedNumbers.CONTENT_URI, values
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
                if (deleteSmsConversation(this@PrivateChatListActivity, threadId.toLong())) {
                    SharedPreferencesHelper.addDeletedThreadId(
                        this@PrivateChatListActivity, threadId.toLong()
                    )
                    val mutableList = privateMessageList.toMutableList()
                    mutableList.removeAll { it.threadId == threadId.toLong() }

                    privateMessageList = mutableList.toList()
                    rvPrivateListAdapter.removeByThreadId(threadId.toLong())

                    if (SharedPreferencesHelper.isPinned(
                            this@PrivateChatListActivity, threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.setPinned(this, threadId.toLong(), false)
                    }
                    if (SharedPreferencesHelper.isThreadArchived(
                            this@PrivateChatListActivity, threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.removeArchivedThread(
                            this@PrivateChatListActivity, threadId.toLong()
                        )
                    }
                    if (SharedPreferencesHelper.isThreadPrivate(
                            this@PrivateChatListActivity, threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.removePrivateThread(
                            this@PrivateChatListActivity, threadId.toLong()
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

    private fun clearSelectionViewOrUpdate() {
        afterApplyFilterUpdateMessages(privateMessageList)
        updateSelectedCount(-1)
        storeThreadIDList.clear()
        SharedPreferencesHelper.saveArrayList(
            this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
        )
        rvPrivateListAdapter.clearAndUpdateView()
    }

    private fun afterApplyFilterUpdateMessages(chatList: List<ChatUser>) {
        val pinnedMessageList = chatList.map {
            it.copy(
                isPinned = SharedPreferencesHelper.isPinned(
                    this@PrivateChatListActivity, it.threadId
                )
            )
        }
            .sortedWith(compareByDescending<ChatUser> { it.isPinned }.thenByDescending { it.timestamp })

        if (pinnedMessageList.isNotEmpty()) {
            binding.rvNoMessageView.visibility = View.GONE
            binding.rvArchivedMessageList.fadeIn()
            rvPrivateListAdapter.updateData(pinnedMessageList)
            binding.paginationProgress.fadeOut()
            binding.rvArchivedMessageList.alpha = 1f
        } else {
            binding.paginationProgress.fadeOut()
            binding.rvArchivedMessageList.visibility = View.GONE
            binding.rvNoMessageView.fadeIn()
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
                        val privateThreadIdMap =
                            SharedPreferencesHelper.getPrivateThreadIds(this@PrivateChatListActivity)
                        val privateThreadIds: List<Long> =
                            privateThreadIdMap.mapNotNull { it.toLongOrNull() }
                        privateMessageList =
                            getThreadsByIds(this@PrivateChatListActivity, privateThreadIds)

                        withContext(Dispatchers.Main) {
                            afterApplyFilterUpdateMessages(privateMessageList)

                            privateMessageList.forEachIndexed { _, item ->
                                val (name, photoUri) = getContactInfo(
                                    this@PrivateChatListActivity, item.address
                                )
                                item.contactName = name
                                item.photoUri = photoUri
                            }
                        }
                    }
                }
            })
    }

    fun updateSelectedCount(selectedCount: Int) {
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
            rvPrivateListAdapter.clearAndUpdateView()
            binding.iSelectionHeader.root.visibility = View.GONE
            binding.rvTopHeaderView.visibility = View.VISIBLE
        }
    }

    private fun getThreadsByIds(
        context: Context, threadIds: List<Long>
    ): List<ChatUser> {
        if (threadIds.isEmpty()) return emptyList()

        val smsThreads = mutableListOf<ChatUser>()
        val contentResolver = context.contentResolver

        val smsUri = "content://sms".toUri()
        val projection = arrayOf("thread_id", "address", "body", "date", "read", "sub_id", "type")

        val selection = "thread_id IN (${threadIds.joinToString(",") { "?" }})"
        val selectionArgs = threadIds.map { it.toString() }.toTypedArray()

        val cursor = contentResolver.query(
            smsUri, projection, selection, selectionArgs, "date DESC"
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

    private fun getContactInfo(context: Context, phoneNumber: String): Pair<String?, String?> {
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI
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

    private fun getPrivateChatsSMS() {
        CoroutineScope(Dispatchers.IO).launch {
            val privateThreadIdMap =
                SharedPreferencesHelper.getPrivateThreadIds(this@PrivateChatListActivity)
            val privateThreadIds: List<Long> = privateThreadIdMap.mapNotNull { it.toLongOrNull() }
            privateMessageList = getThreadsByIds(this@PrivateChatListActivity, privateThreadIds)

            withContext(Dispatchers.Main) {
                afterApplyFilterUpdateMessages(privateMessageList)
                privateMessageList.forEachIndexed { _, item ->
                    val (name, photoUri) = getContactInfo(
                        this@PrivateChatListActivity, item.address
                    )
                    item.contactName = name
                    item.photoUri = photoUri
                }
            }
        }
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
                        getSmsChatUserByThreadId(this@PrivateChatListActivity, lastThreadID)

                    withContext(Dispatchers.Main) {
                        updatedRecord?.let { record ->
                            rvPrivateListAdapter.updateItemByThreadId(lastThreadID, record)
                        }
                    }
                }
            }
            getPrivateChatsSMS()
        }
        super.onResume()
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
                showAd(this@PrivateChatListActivity) {
                    val intent =
                        Intent(this@PrivateChatListActivity, PersonalChatActivity::class.java)
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

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val viewRect = Rect()
        binding.iSelectionHeader.cvMoreOptionsDialog.getGlobalVisibleRect(viewRect)
        if (binding.iSelectionHeader.cvMoreOptionsDialog.isVisible && !viewRect.contains(
                ev.rawX.toInt(), ev.rawY.toInt()
            )
        ) {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            return true
        }
        return super.dispatchTouchEvent(ev)
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
            Telephony.Sms.SUBSCRIPTION_ID,
            Telephony.Sms.TYPE
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
            val idxType = it.getColumnIndex(Telephony.Sms.TYPE)

            while (it.moveToNext()) {
                val date = it.getLong(idxDate)
                val isRead = it.getInt(idxRead) == 1
                val type = if (idxType >= 0) it.getInt(idxType) else -1

                // first message (newest) will set latest info
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
            isPinned = isPinned,
            photoUri = "",
            unreadCount = unreadCount,
            simSlot = simSlot
        )
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
                rvPrivateListAdapter.updateMarkAsRead(threadId)
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

            "REMOVE_FROM_PRIVATE" -> {
                SharedPreferencesHelper.removePrivateThread(this, threadId)
                val mutableList = privateMessageList.toMutableList()
                mutableList.removeAll { it.threadId == threadId }

                privateMessageList = mutableList.toList()
                rvPrivateListAdapter.removeByThreadId(threadId)

                Handler(Looper.getMainLooper()).postDelayed({
                    CallbackHolder.listener?.onUpdateTheRecyclerView(true)
                    clearSelectionViewOrUpdate()
                }, 100)
            }

            "BLOCKED" -> {
                val contactInfo =
                    getPhoneNumberOrAddressFromThreadId(
                        this@PrivateChatListActivity,
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