package com.chat.sms_text.messages.activity

import android.app.Dialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.ads.BannerAdHelper
import com.chat.sms_text.messages.ads.BannerType
import com.chat.sms_text.messages.ads.InterstitialAdHelper
import com.chat.sms_text.messages.ads.NativeAdHelper
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ActivityChatInfoBinding
import com.chat.sms_text.messages.databinding.DialogBlockNumberBinding
import com.chat.sms_text.messages.databinding.DialogDeleteConversationBinding
import com.chat.sms_text.messages.model.ContactInfo
import com.chat.sms_text.messages.utils.getColorFromAttr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatInfoActivity : BaseActivity() {
    private lateinit var binding: ActivityChatInfoBinding
    private var contactUserDetails: ContactInfo? = null
    private var threadId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        runAdsCampion()
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@ChatInfoActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChatInfoActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChatInfoActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChatInfoActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@ChatInfoActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("ChatInfoActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ChatInfoActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ChatInfoActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ChatInfoActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        loadAd(this@ChatInfoActivity)
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
        if (intent.hasExtra(Const.IS_SENDER_OR_NUMBER) &&
            intent.hasExtra(Const.SENDER_OR_NUMBER) &&
            intent.hasExtra(Const.THREAD_ID)
        ) {
            threadId = intent.getLongExtra(Const.THREAD_ID, 0)
            val senderIdOrNumber = intent.getStringExtra(Const.SENDER_OR_NUMBER)
            val isSenderOrNumber = intent.getBooleanExtra(Const.IS_SENDER_OR_NUMBER, true)

            contactUserDetails = if (isSenderOrNumber) {
                senderIdOrNumber?.let { getContactInfoFromSenderId(this, it) }
            } else {
                senderIdOrNumber?.let { getContactInfoFromPhoneNumber(this, it) }
            }

            if (contactUserDetails != null) {
                binding.txtUserName.text = contactUserDetails?.name
                binding.txtUserNumber.text = contactUserDetails?.number
            } else {
                binding.txtUserName.text = senderIdOrNumber
            }

            updateProfile()

            if (binding.rvBlock.isVisible) {
                val originalNumber = getPhoneNumberOrAddressFromThreadId(this, threadId)
                if (isAddressBlocked(this, originalNumber.toString())) {
                    binding.txtBlockOrUnblock.text = resources.getString(R.string.unblock)
                } else binding.txtBlockOrUnblock.text = resources.getString(R.string.block)
            }

            if (SharedPreferencesHelper.isThreadArchived(this, threadId)) {
                binding.txtArchivedOrUnarchived.text = resources.getString(R.string.unarchived)
            } else {
                binding.txtArchivedOrUnarchived.text = resources.getString(R.string.archived)
            }
        }
    }

    private fun getPhoneNumberCorrectionDigits(number: String): String {
        val digitsOnly = number.filter { it.isDigit() }

        return if (digitsOnly.length > 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }

    private fun getContactInfoFromPhoneNumber(context: Context, phoneNumber: String): ContactInfo? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUriString =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                return ContactInfo(name, phoneNumber, photoUriString)
            }
        }
        return null
    }

    private fun getContactInfoFromSenderId(context: Context, senderId: String): ContactInfo? {
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(senderId)
        )

        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.NUMBER,
            ContactsContract.PhoneLookup.PHOTO_URI
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val number =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.NUMBER))
                        ?.trim()
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                val photoUri =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI))
                return ContactInfo(name, number.toString(), photoUri)
            }
        }

        return null
    }

    private fun openThreadNotificationChannelSettings(context: Context, threadId: Long) {
        val channelId = "$threadId"
        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        }
        context.startActivity(intent)
    }

    private fun initClickListener() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val adsEnabled = SharedPreferencesHelper.getBoolean(
                    this@ChatInfoActivity, Const.IS_ADS_ENABLED, false
                )
                val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                    this@ChatInfoActivity, Const.IS_INTERSTITIAL_ENABLED, false
                )

                if (adsEnabled && interstitialEnabled) {
                    InterstitialAdHelper.showAd(this@ChatInfoActivity) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvCall.setOnClickListener {
            val phoneNumber =
                getPhoneNumberFromThreadId(this@ChatInfoActivity, threadId)
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

        binding.rvDeleteConversation.setOnClickListener {
            showDeleteConversationDialog()
        }

        binding.rvNotification.setOnClickListener {
            createNotificationChannelForThread(this, threadId)
            openThreadNotificationChannelSettings(this, threadId)
        }

        binding.rvArchive.setOnClickListener {
            if (binding.txtArchivedOrUnarchived.text == resources.getString(R.string.archived)) {
                SharedPreferencesHelper.saveArchivedThread(this, threadId)
                binding.txtArchivedOrUnarchived.text = resources.getString(R.string.unarchived)
            } else {
                SharedPreferencesHelper.removeArchivedThread(this, threadId)
                binding.txtArchivedOrUnarchived.text = resources.getString(R.string.archived)
            }
        }

        binding.rvBlock.setOnClickListener {
            val originalNumber = getPhoneNumberOrAddressFromThreadId(this, threadId)
            if (binding.txtBlockOrUnblock.text == resources.getString(R.string.block)) {
                showBlockOrUnblockDialog(
                    true,
                    getPhoneNumberCorrectionDigits(originalNumber.toString())
                )
            } else showBlockOrUnblockDialog(
                false,
                getPhoneNumberCorrectionDigits(originalNumber.toString())
            )
        }
    }

    private fun createNotificationChannelForThread(context: Context, threadId: Long) {
        val channelName = binding.txtUserName.text

        val channel = NotificationChannel(
            threadId.toString(), channelName,
            NotificationManager.IMPORTANCE_HIGH
        )

        val notificationManager =
            context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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

    private fun showDeleteConversationDialog() {
        val dialog = Dialog(this)
        val deleteConversationDialogBinding: DialogDeleteConversationBinding =
            DialogDeleteConversationBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(deleteConversationDialogBinding.root)

        dialog.window?.apply {
            setBackgroundDrawableResource(R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
            val metrics = resources.displayMetrics
            setLayout((metrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        deleteConversationDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        deleteConversationDialogBinding.btnYes.setOnClickListener {
            dialog.dismiss()

            lifecycleScope.launch(Dispatchers.IO) {
                val isDeleted = deleteSmsConversation(this@ChatInfoActivity, threadId)

                withContext(Dispatchers.Main) {
                    if (isDeleted) {
                        SharedPreferencesHelper.addDeletedThreadId(
                            this@ChatInfoActivity,
                            threadId
                        )

                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }

        if (!isFinishing && !isDestroyed) dialog.show()
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

    private fun isAddressBlocked(context: Context?, number: String): Boolean {
        return try {
            BlockedNumberContract.isBlocked(context, number)
        } catch (_: Exception) {
            false
        }
    }

    private fun showBlockOrUnblockDialog(isBlocked: Boolean = false, phoneNumber: String) {
        val dialog = Dialog(this)
        val dialogBlockOrUnblockBinding: DialogBlockNumberBinding =
            DialogBlockNumberBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBlockOrUnblockBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        if (isBlocked) {
            dialogBlockOrUnblockBinding.rvBlockNumber.visibility = View.GONE
            dialogBlockOrUnblockBinding.txtStatement.visibility = View.VISIBLE
            val titleTxt = resources.getString(R.string.block) + " " + phoneNumber + " ?"
            val subTitleTxt =
                resources.getString(R.string.are_you_sure_you_want_to_block_this_number)
            dialogBlockOrUnblockBinding.btnYes.text = resources.getString(R.string.block)
            dialogBlockOrUnblockBinding.txtStatement.text = subTitleTxt
            dialogBlockOrUnblockBinding.txtTitle.text = titleTxt
        } else {
            dialogBlockOrUnblockBinding.rvBlockNumber.visibility = View.GONE
            dialogBlockOrUnblockBinding.txtStatement.visibility = View.VISIBLE
            val titleTxt = resources.getString(R.string.unblock) + " " + phoneNumber + " ?"
            val subTitleTxt =
                resources.getString(R.string.are_you_sure_you_want_to_unblock_this_number)
            dialogBlockOrUnblockBinding.txtTitle.text = titleTxt
            dialogBlockOrUnblockBinding.txtStatement.text = subTitleTxt
            dialogBlockOrUnblockBinding.btnYes.text = resources.getString(R.string.unblock)
        }

        dialogBlockOrUnblockBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBlockOrUnblockBinding.btnYes.setOnClickListener {
            val contactInfo = getPhoneNumberOrAddressFromThreadId(
                this@ChatInfoActivity,
                threadId
            )
            if (isBlocked) {
                if (blockNumber(this, contactInfo.toString())) {
                    SharedPreferencesHelper.addToBlockList(
                        this@ChatInfoActivity,
                        contactUserDetails?.number.toString()
                    )
                    updateProfile()
                    showToast(getString(R.string.contact_has_been_blocked_successfully))
                    binding.txtBlockOrUnblock.text = resources.getString(R.string.unblock)
                }
            } else {
                if (unblockNumber(this, contactInfo.toString())) {
                    updateProfile()
                    showToast(getString(R.string.contact_has_been_unblocked_successfully))
                    binding.txtBlockOrUnblock.text = resources.getString(R.string.block)
                }
            }
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

    private fun updateProfile() {
        val originalNumber = getPhoneNumberOrAddressFromThreadId(this, threadId)
        if (isAddressBlocked(this, originalNumber.toString())) {
            binding.cvProfileView.setCardBackgroundColor(getColor(R.color.blocked_user_profile))
            binding.ivOriginalProfile.visibility = View.GONE
            binding.ivDefaultProfile.visibility = View.GONE
            binding.ivBlockProfile.visibility = View.VISIBLE
        } else {
            if (contactUserDetails != null) {
                if (contactUserDetails?.photoUri != null && !contactUserDetails?.photoUri.contentEquals(
                        "null"
                    )
                ) {
                    binding.ivDefaultProfile.visibility = View.GONE
                    binding.ivBlockProfile.visibility = View.GONE
                    binding.ivOriginalProfile.visibility = View.VISIBLE
                    Glide.with(this).load(contactUserDetails?.photoUri?.toUri())
                        .into(binding.ivOriginalProfile)
                } else {
                    if (SharedPreferencesHelper.getBoolean(
                            this,
                            Const.IS_CHANGE_PROFILE_COLOR,
                            false
                        )
                    ) {
                        binding.ivDefaultProfile.setImageDrawable(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.ic_profile
                            )
                        )
                    } else {
                        binding.ivDefaultProfile.setImageDrawable(
                            ContextCompat.getDrawable(
                                this,
                                R.drawable.ic_dark_profile_popup
                            )
                        )
                    }
                    binding.ivOriginalProfile.visibility = View.GONE
                    binding.ivBlockProfile.visibility = View.GONE
                    binding.cvProfileView.setCardBackgroundColor(
                        ColorStateList.valueOf(
                            getColorFromAttr(R.attr.itemBackgroundColor)
                        )
                    )
                    binding.ivDefaultProfile.visibility = View.VISIBLE
                }
            } else {
                if (SharedPreferencesHelper.getBoolean(
                        this,
                        Const.IS_CHANGE_PROFILE_COLOR,
                        false
                    )
                ) {
                    binding.ivDefaultProfile.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_profile
                        )
                    )
                } else {
                    binding.ivDefaultProfile.setImageDrawable(
                        ContextCompat.getDrawable(
                            this,
                            R.drawable.ic_dark_profile_popup
                        )
                    )
                }
                binding.ivOriginalProfile.visibility = View.GONE
                binding.ivBlockProfile.visibility = View.GONE
                binding.cvProfileView.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(R.attr.itemBackgroundColor)
                    )
                )
                binding.ivDefaultProfile.visibility = View.VISIBLE
            }
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

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }
}