package com.texting.sms.messaging_app.activity

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RadioButton
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
import com.texting.sms.messaging_app.adapter.ScheduledMessageAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.ScheduledSMSDatabaseHelper
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityScheduledMessageBinding
import com.texting.sms.messaging_app.databinding.DialogScheduledMessageBinding
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.listener.OnScheduledClickInterface
import com.texting.sms.messaging_app.model.ChatModel
import com.texting.sms.messaging_app.model.ScheduledSms
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduledMessageActivity : BaseActivity(), OnScheduledClickInterface,
    NetworkAvailableListener {
    private lateinit var binding: ActivityScheduledMessageBinding
    private lateinit var rvScheduledMessageListAdapter: ScheduledMessageAdapter
    private var scheduledMessageList: MutableList<ScheduledSms> = mutableListOf()

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scheduled_message)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@ScheduledMessageActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ScheduledMessageActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ScheduledMessageActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@ScheduledMessageActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("ScheduledMessageActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ScheduledMessageActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ScheduledMessageActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ScheduledMessageActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
        val layoutManagerMessage: RecyclerView.LayoutManager =
            LinearLayoutManager(this@ScheduledMessageActivity)
        binding.rvScheduledChatView.setLayoutManager(layoutManagerMessage)
        rvScheduledMessageListAdapter = ScheduledMessageAdapter(
            mutableListOf(),
            this,
            this@ScheduledMessageActivity
        )
        binding.rvScheduledChatView.adapter = rvScheduledMessageListAdapter
        (binding.rvScheduledChatView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        lifecycleScope.launch {
            refreshScheduledList()
        }
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvStartChat.setOnClickListener {
            val intent = Intent(this, NewConversationActivity::class.java)
            intent.putExtra(Const.FROM_PAGE, Const.SCHEDULED_MESSAGE)
            startActivity(intent)
        }
    }

    override fun onItemClick(scheduledSMS: ScheduledSms) {
        showScheduleMessageDialog(scheduledSMS)
    }

    private fun showScheduleMessageDialog(scheduledSMS: ScheduledSms) {
        val dialog = Dialog(this)
        val dialogScheduledMessageBinding: DialogScheduledMessageBinding =
            DialogScheduledMessageBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogScheduledMessageBinding.root)

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

        var performAction = Const.STRING_DEFAULT_VALUE

        dialogScheduledMessageBinding.rgScheduleMessageSelection.setOnCheckedChangeListener { _, checkedId ->
            val selectedValue = dialog.findViewById<RadioButton>(checkedId)
            if (selectedValue != null) {
                performAction = selectedValue.text.toString()
            }
        }

        dialogScheduledMessageBinding.btnYes.setOnClickListener {
            when (performAction) {
                Const.STRING_DEFAULT_VALUE -> {
                    showToast(getString(R.string.please_select_action))
                }

                resources.getString(R.string.send_now) -> {
                    dialog.dismiss()

                    lifecycleScope.launch {
                        binding.paginationProgress.fadeIn()

                        withContext(Dispatchers.IO) {
                            sendSMSAndSimulateMMS(
                                this@ScheduledMessageActivity,
                                scheduledSMS.number,
                                scheduledSMS.message,
                                scheduledSMS.imageURIs
                            )

                            val dbHelper = ScheduledSMSDatabaseHelper(this@ScheduledMessageActivity)
                            dbHelper.deleteSms(scheduledSMS.id)
                        }

                        refreshScheduledList()

                        showToast(
                            getString(R.string.scheduled_message_sent_successfully)
                        )
                    }
                }

                resources.getString(R.string.copy_text) -> {
                    dialog.dismiss()
                    if (scheduledSMS.message.isNotEmpty()) {
                        val clipboard =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Copied Text", scheduledSMS.message)
                        clipboard.setPrimaryClip(clip)
                    } else {
                        showToast(getString(R.string.no_message_text_available))
                    }
                }

                resources.getString(R.string.delete) -> {
                    dialog.dismiss()

                    lifecycleScope.launch {
                        binding.paginationProgress.fadeIn()

                        withContext(Dispatchers.IO) {
                            val dbHelper =
                                ScheduledSMSDatabaseHelper(this@ScheduledMessageActivity)
                            dbHelper.deleteSms(scheduledSMS.id)
                        }

                        refreshScheduledList()

                        showToast(
                            getString(R.string.scheduled_message_delete_successfully)
                        )
                    }
                }
            }
        }

        dialogScheduledMessageBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private suspend fun refreshScheduledList() {
        val updatedList = withContext(Dispatchers.IO) {
            val dbHelper =
                ScheduledSMSDatabaseHelper(this@ScheduledMessageActivity)
            val currentTime = System.currentTimeMillis()

            dbHelper.getAllSms().filter { sms ->
                if (sms.scheduledMillis > currentTime) {
                    true
                } else {
                    dbHelper.deleteSms(sms.id)
                    false
                }
            }
        }

        scheduledMessageList = updatedList.toMutableList()

        if (scheduledMessageList.isNotEmpty()) {
            rvScheduledMessageListAdapter.updateData(scheduledMessageList)
            binding.llNoScheduledMessage.visibility = View.GONE
            binding.rvScheduledChatView.fadeIn()
        } else {
            binding.rvScheduledChatView.visibility = View.GONE
            binding.llNoScheduledMessage.fadeIn()
        }

        binding.paginationProgress.visibility = View.GONE
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
                    SharedPreferencesHelper.getInt(this, Const.SIM_SLOT_NUMBER, defaultSmsSubId)
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
            this,
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
                put("msg_box", 2)
                put("m_type", 128)
                put("ct_t", "application/vnd.wap.multipart.related")
                put("sub", "")
            }

            val mmsUri = context.contentResolver.insert("content://mms".toUri(), mmsValues)
                ?: throw Exception("Failed to insert MMS")

            val messageId = ContentUris.parseId(mmsUri)

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
                    put("seq", index)
                }

                val partUri = context.contentResolver.insert(
                    "content://mms/$messageId/part".toUri(),
                    partValues
                ) ?: throw Exception("Failed to insert MMS part")

                context.contentResolver.openOutputStream(partUri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                }
            }
        } catch (e: Exception) {
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

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        } else {
            initView()
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