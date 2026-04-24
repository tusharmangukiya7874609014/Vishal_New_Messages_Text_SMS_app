package com.texting.sms.messaging_app.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityStarredMessagesBinding
import com.texting.sms.messaging_app.listener.CallbackHolder
import com.texting.sms.messaging_app.listener.OnStarredImageClick
import com.texting.sms.messaging_app.listener.OnStarredLinkClick
import com.texting.sms.messaging_app.listener.OnStarredTextClick
import com.texting.sms.messaging_app.model.ContactInfo
import com.texting.sms.messaging_app.model.MessageDetails
import com.texting.sms.messaging_app.model.MmsContactInfo
import com.texting.sms.messaging_app.utils.StarCategory
import com.texting.sms.messaging_app.adapter.StarredImagesAdapter
import com.texting.sms.messaging_app.adapter.StarredLinkAdapter
import com.texting.sms.messaging_app.adapter.StarredPlainMessageAdapter
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.InterstitialAdHelper.loadAd
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StarredMessagesActivity : BaseActivity(), OnStarredTextClick, OnStarredLinkClick,
    OnStarredImageClick, NetworkAvailableListener {
    private lateinit var binding: ActivityStarredMessagesBinding
    private lateinit var rvStarredMessageAdapter: StarredPlainMessageAdapter
    private lateinit var rvStarredImagesAdapter: StarredImagesAdapter
    private lateinit var rvStarredLinkAdapter: StarredLinkAdapter
    private lateinit var planStarredMessageList: List<MessageDetails>
    private var contactUserDetails: ContactInfo? = null
    private lateinit var onStarredTextClick: OnStarredTextClick
    private lateinit var onStarredLinkClick: OnStarredLinkClick
    private lateinit var onStarredImageClick: OnStarredImageClick
    private var threadId = 0L

    private lateinit var networkUtil: NetworkConnectionUtil

    override fun onStart() {
        super.onStart()
        networkUtil.register()
    }

    override fun onStop() {
        super.onStop()
        networkUtil.unregister()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkUtil = NetworkConnectionUtil(this)
        networkUtil.setListener(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_starred_messages)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onStarredTextClick = this
        onStarredLinkClick = this
        onStarredImageClick = this
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@StarredMessagesActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@StarredMessagesActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@StarredMessagesActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@StarredMessagesActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("StarredMessagesActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@StarredMessagesActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@StarredMessagesActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@StarredMessagesActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val bannerAdsType = getAdsPageResponse.optString("bannerAdsType").ifEmpty {
                "adaptive"
            } ?: "adaptive"

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isCurrentPageNativeAdsEnabled && !isCurrentPageBannerAdsEnabled) {
                    if (binding.nativeAdContainer.isVisible) return@withContext

                    runNativeAds(
                        nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
                    )
                } else if (isCurrentPageBannerAdsEnabled && !isCurrentPageNativeAdsEnabled) {
                    if (binding.bannerAdContainer.root.isVisible) return@withContext

                    runBannerAds(
                        bannerAdsId = currentPageBannerAdsID, bannerAdsType = bannerAdsType
                    )
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
        if (intent.hasExtra(Const.IS_SENDER_OR_NUMBER) && intent.hasExtra(Const.SENDER_OR_NUMBER) && intent.hasExtra(
                Const.THREAD_ID
            )
        ) {
            threadId = intent.getLongExtra(Const.THREAD_ID, 0)
            val senderIdOrNumber = intent.getStringExtra(Const.SENDER_OR_NUMBER)
            val isSenderOrNumber = intent.getBooleanExtra(Const.IS_SENDER_OR_NUMBER, true)

            contactUserDetails = if (isSenderOrNumber) {
                senderIdOrNumber?.let { getContactInfoFromSenderId(this, it) }
            } else {
                senderIdOrNumber?.let { getContactInfoFromPhoneNumber(this, it) }
            }

            val starredMessagesID = SharedPreferencesHelper.getStarredMessages(
                this,
                StarCategory.TEXT_ONLY,
                threadId.toString()
            )

            val starredImagesID = SharedPreferencesHelper.getStarredMessages(
                this,
                StarCategory.IMAGE,
                threadId.toString()
            )

            val starredLinkMessageID = SharedPreferencesHelper.getStarredMessages(
                this,
                StarCategory.LINK,
                threadId.toString()
            )

            if (starredMessagesID.isEmpty() && starredImagesID.isEmpty() && starredLinkMessageID.isEmpty()) {
                binding.llMainStarredView.visibility = View.GONE
                binding.rvNoMessageView.fadeIn()
            } else {
                binding.rvNoMessageView.visibility = View.GONE
                binding.llMainStarredView.fadeIn()
            }

            if (starredMessagesID.isNotEmpty()) {
                binding.txtTextTitle.visibility = View.VISIBLE
                val layoutManager = object : LinearLayoutManager(this) {
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }
                binding.rvPlainStarredMessage.setLayoutManager(layoutManager)

                CoroutineScope(Dispatchers.IO).launch {
                    planStarredMessageList =
                        getMessagesDetails(this@StarredMessagesActivity, starredMessagesID)

                    withContext(Dispatchers.Main) {
                        rvStarredMessageAdapter = StarredPlainMessageAdapter(
                            planStarredMessageList,
                            onStarredTextClick,
                            this@StarredMessagesActivity
                        )
                        binding.rvPlainStarredMessage.adapter = rvStarredMessageAdapter
                    }
                }
            } else {
                binding.txtTextTitle.visibility = View.GONE
                binding.rvPlainStarredMessage.visibility = View.GONE
            }

            if (starredImagesID.isNotEmpty()) {
                binding.txtImages.visibility = View.VISIBLE
                val layoutManager: RecyclerView.LayoutManager =
                    LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                binding.rvImagesStarredMessage.setLayoutManager(layoutManager)

                rvStarredImagesAdapter =
                    StarredImagesAdapter(
                        starredImagesID,
                        onStarredImageClick,
                        this@StarredMessagesActivity
                    )
                binding.rvImagesStarredMessage.adapter = rvStarredImagesAdapter
                binding.rvImagesStarredMessage.fadeIn()
            } else {
                binding.txtImages.visibility = View.GONE
                binding.rvImagesStarredMessage.visibility = View.GONE
            }

            if (starredLinkMessageID.isNotEmpty()) {
                binding.txtLink.visibility = View.VISIBLE
                val layoutManager = object : LinearLayoutManager(this) {
                    override fun canScrollVertically(): Boolean {
                        return false
                    }
                }
                binding.rvLinksStarredMessage.setLayoutManager(layoutManager)

                CoroutineScope(Dispatchers.IO).launch {
                    planStarredMessageList =
                        getMessagesDetails(this@StarredMessagesActivity, starredLinkMessageID)

                    withContext(Dispatchers.Main) {
                        rvStarredLinkAdapter =
                            StarredLinkAdapter(
                                planStarredMessageList,
                                onStarredLinkClick,
                                this@StarredMessagesActivity
                            )
                        binding.rvLinksStarredMessage.adapter = rvStarredLinkAdapter
                    }
                }
            } else {
                binding.txtLink.visibility = View.GONE
                binding.rvLinksStarredMessage.visibility = View.GONE
            }
        }
    }

    private fun getMessagesDetails(
        context: Context,
        messageIds: MutableSet<String>
    ): List<MessageDetails> {
        val messages = mutableListOf<MessageDetails>()

        for (id in messageIds) {
            val smsUri = "content://sms/$id".toUri()
            val projection = arrayOf("_id", "address", "body", "date", "type")

            context.contentResolver.query(smsUri, projection, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                    val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                    val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                    val type = cursor.getInt(cursor.getColumnIndexOrThrow("type"))

                    messages.add(
                        MessageDetails(
                            id = id.toLong(),
                            address = address,
                            body = body,
                            date = date,
                            type = type,
                            contactName = contactUserDetails?.name,
                            photoUri = contactUserDetails?.photoUri
                        )
                    )
                }
            }
        }
        return messages
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

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onPlainTextMessageClick(
        messageDetails: MessageDetails,
        actionOfTask: String
    ) {
        when (actionOfTask) {
            "CALL" -> {
                if (messageDetails.address.isNullOrEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        messageDetails.address
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:${messageDetails.address}".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(dialIntent)
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to start dialer", e)
                    }
                }
            }

            "VIEW_IN_CHAT" -> {
                CallbackHolder.highlightViewListener?.onItemClick(messageDetails.id.toString())
                onBackPressedDispatcher.onBackPressed()
            }

            "SEND_MESSAGES" -> {
                onBackPressedDispatcher.onBackPressed()
            }

            "COPY_MESSAGE" -> {
                copyTextToClipboard(this, messageDetails.body.toString())
            }

            "WHATSAPP_MESSAGE" -> {
                if (messageDetails.address.isNullOrEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        messageDetails.address
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    sendMessageUsingWhatsAppPackage(messageDetails.address)
                }
            }
        }
    }

    private fun sendMessageUsingWhatsAppPackage(number: String) {
        try {
            val uri = "smsto:$number".toUri()
            val i = Intent(Intent.ACTION_SENDTO, uri)
            i.setPackage("com.whatsapp")
            startActivity(Intent.createChooser(i, ""))
        } catch (e: Exception) {
            showToast(getString(R.string.whatsapp_not_installed))
            e.printStackTrace()
        }
    }

    private fun copyTextToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboard.setPrimaryClip(clip)

        showToast(getString(R.string.copied_to_clipboard))
    }

    override fun onLinkMessageClick(
        messageDetails: MessageDetails,
        actionOfTask: String
    ) {
        when (actionOfTask) {
            "CALL" -> {
                if (messageDetails.address.isNullOrEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        messageDetails.address
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:${messageDetails.address}".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(dialIntent)
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to start dialer", e)
                    }
                }
            }

            "OPEN" -> {
                val linkMessage = extractLinkFromMessage(messageDetails.body.toString())
                openLink(this, linkMessage.toString())
            }

            "VIEW_IN_CHAT" -> {
                CallbackHolder.highlightViewListener?.onItemClick(messageDetails.id.toString())
                onBackPressedDispatcher.onBackPressed()
            }

            "SEND_MESSAGES" -> {
                onBackPressedDispatcher.onBackPressed()
            }

            "COPY_LINK" -> {
                val linkMessage = extractLinkFromMessage(messageDetails.body.toString())
                copyTextToClipboard(this, linkMessage.toString())
            }

            "WHATSAPP_MESSAGE" -> {
                if (messageDetails.address.isNullOrEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        messageDetails.address
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    sendMessageUsingWhatsAppPackage(messageDetails.address)
                }
            }
        }
    }

    private fun openLink(context: Context, url: String) {
        try {
            val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }

            val intent = Intent(Intent.ACTION_VIEW, fixedUrl.toUri())
            context.startActivity(intent)
        } catch (_: Exception) {
            showToast(getString(R.string.no_application_found_to_open_this_link))
        }
    }

    private fun extractLinkFromMessage(message: String): String? {
        val urlPattern =
            "((https?://|http://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}([/?#]\\S*)?)"
                .toRegex(RegexOption.IGNORE_CASE)
        return urlPattern.find(message)?.value
    }

    override fun onImagesClick(
        messageDetails: String,
        actionOfTask: String,
        messageDetailsView: MmsContactInfo
    ) {
        when (actionOfTask) {
            "CALL" -> {
                if (messageDetailsView.address.isNullOrEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        messageDetailsView.address
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:${messageDetailsView.address}".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(dialIntent)
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to start dialer", e)
                    }
                }
            }

            "SEND_MESSAGES" -> {
                onBackPressedDispatcher.onBackPressed()
            }

            "VIEW_IN_CHAT" -> {
                CallbackHolder.highlightViewListener?.onItemClick("Images : $messageDetails")
                onBackPressedDispatcher.onBackPressed()
            }

            "WHATSAPP_MESSAGE" -> {
                if (messageDetailsView.address.isNullOrEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        messageDetailsView.address
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    sendMessageUsingWhatsAppPackage(messageDetailsView.address)
                }
            }
        }
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }

    override fun onNetworkAvailable() {
        runOnUiThread {
            val sharePreference = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)

            val purposeConsents = sharePreference.getString("IABTCF_PurposeConsents", "")
            if (!purposeConsents.isNullOrEmpty()) {
                val purposeOneString = purposeConsents.first().toString()
                val hasConsentForPurposeOne = purposeOneString == "1"

                if (hasConsentForPurposeOne) runAdsCampion()
            } else {
                runAdsCampion()
            }
        }
    }

    override fun onNetworkLost() {

    }
}