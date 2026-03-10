package com.chat.sms_text.messages.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.ActivityAllConversationsBinding
import com.chat.sms_text.messages.adapter.AllConversationAdapter
import com.chat.sms_text.messages.ads.NativeAdHelper
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.listener.CallbackHolder
import com.chat.sms_text.messages.listener.OnChatUserInterface
import com.chat.sms_text.messages.model.ChatUser
import com.chat.sms_text.messages.utils.getDrawableFromAttr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.isNotEmpty
import kotlin.collections.iterator

class AllConversationsActivity : BaseActivity(), OnChatUserInterface {
    private lateinit var binding: ActivityAllConversationsBinding
    private lateinit var rvMessageListAdapter: AllConversationAdapter
    private var privateThreadIDList = ArrayList<String>()
    private var allMessageList = emptyList<ChatUser>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_all_conversations)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", -1L)
        runAdsCampion()
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@AllConversationsActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AllConversationsActivity, Const.IS_NATIVE_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@AllConversationsActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("AllConversationsActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AllConversationsActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AllConversationsActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isCurrentPageNativeAdsEnabled) {
                    runNativeAds(
                        nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
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

    private fun initView() {
        val layoutManagerMessage: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvMessageList.setLayoutManager(layoutManagerMessage)
        rvMessageListAdapter = AllConversationAdapter(
            mutableListOf(),
            privateThreadIDList,
            this,
            getActiveSimCount(this),
            this
        )
        binding.rvMessageList.adapter = rvMessageListAdapter
        binding.rvMessageList.setHasFixedSize(true)
        binding.rvMessageList.setItemViewCacheSize(20)
        binding.rvMessageList.isNestedScrollingEnabled = false
        (binding.rvMessageList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        receiverSMSOrMMS()
        getSMSResponse()
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

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            val privateThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.PRIVATE_MESSAGE_IDS)
            if (privateThreadID.isNotEmpty()) {
                binding.ivBack.setImageDrawable(getDrawableFromAttr(R.attr.backIcon))
                binding.ivSelectMark.alpha = 0.5f
                binding.txtTitle.text = resources.getString(R.string.conversation)
                updateSelectedCount(0)
                rvMessageListAdapter.clearAndUpdateView()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.ivSelectMark.setOnClickListener {
            val privateThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.PRIVATE_MESSAGE_IDS)
            if (privateThreadID.isNotEmpty()) {
                privateThreadID.forEach {
                    SharedPreferencesHelper.savePrivateThread(this, it.toLong())
                }
                CallbackHolder.listener?.onUpdateTheRecyclerView(true)
                onBackPressedDispatcher.onBackPressed()
            } else {
                showToast(getString(R.string.first_select_message_atleast_one_or_more))
            }
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
                        allMessageList = getAllSmsThreads(this@AllConversationsActivity)

                        withContext(Dispatchers.Main) {
                            afterApplyFilterUpdateMessages(allMessageList)
                        }
                    }
                }
            }
        )
    }

    private fun afterApplyFilterUpdateMessages(chatList: List<ChatUser>) {
        val finalChatsList = SharedPreferencesHelper.filterNonPrivateThreads(
            this@AllConversationsActivity,
            chatList
        )

        if (finalChatsList.isNotEmpty()) {
            binding.rvNoMessageView.visibility = View.GONE
            binding.paginationProgress.visibility = View.GONE
            rvMessageListAdapter.updateData(finalChatsList)
            binding.rvMessageList.fadeIn()
        } else {
            binding.rvMessageList.visibility = View.GONE
            binding.paginationProgress.visibility = View.GONE
            binding.rvNoMessageView.fadeIn()
        }
    }

    private fun getSMSResponse() {
        CoroutineScope(Dispatchers.IO).launch {
            allMessageList = getAllSmsThreads(this@AllConversationsActivity)

            withContext(Dispatchers.Main) {
                afterApplyFilterUpdateMessages(allMessageList)
            }
        }
    }

    private fun getAllSmsThreads(context: Context): List<ChatUser> {
        val smsThreads = mutableListOf<ChatUser>()
        val contentResolver = context.contentResolver

        val smsUri = "content://sms".toUri()
        val projection = arrayOf("thread_id", "address", "body", "date", "read", "sub_id", "type")
        val cursor = contentResolver.query(
            smsUri,
            projection,
            null,
            null,
            "date DESC"
        )

        val threadMap =
            mutableMapOf<Long, MutableList<SmsPart>>()
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
                list.add(SmsPart(address, body, date, isRead, simSlot = simSlot, type))
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
                } else {
                    break
                }
            }

            val latestMessage = sortedMessages.first()
            latestMergedMessage = buffer.toString()
            val previewText = if (latestMessage.type == 2) {
                getString(R.string.you, latestMergedMessage)
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

    fun updateSelectedCount(selectedCount: Int) {
        if (selectedCount != 0) {
            val finalMessage = getString(R.string.selected, selectedCount.toString())
            binding.txtTitle.text = finalMessage
            binding.ivSelectMark.alpha = 1f
            binding.ivBack.setImageDrawable(getDrawableFromAttr(R.attr.closeIconSelect))
        } else {
            privateThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this,
                Const.PRIVATE_MESSAGE_IDS,
                privateThreadIDList
            )
            binding.ivBack.setImageDrawable(getDrawableFromAttr(R.attr.backIcon))
            binding.ivSelectMark.alpha = 0.5f
            binding.txtTitle.text = resources.getString(R.string.conversation)
            rvMessageListAdapter.clearAndUpdateView()
        }
    }

    private fun getContactInfo(context: Context, phoneNumber: String): Pair<String?, String?> {
        val contentResolver = context.contentResolver
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
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

    override fun chatUserClick(userChatDetails: ChatUser) {}

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        } else {
            val lastThreadID = SharedPreferencesHelper.getLong(this, "CURRENT_THREAD_ID", -1L)
            if (lastThreadID != -1L) {
                privateThreadIDList.clear()
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedRecord =
                        getSmsChatUserByThreadId(this@AllConversationsActivity, lastThreadID)

                    withContext(Dispatchers.Main) {
                        updatedRecord?.let { record ->
                            val allMessagePosition =
                                allMessageList.indexOfFirst { it.threadId == record.threadId }
                            if (allMessagePosition != -1) {
                                val mutableList = allMessageList.toMutableList()
                                mutableList[allMessagePosition] = record
                                allMessageList = mutableList.toList()
                            }

                            rvMessageListAdapter.updateItemByThreadId(lastThreadID, record)
                        }
                    }
                }
                SharedPreferencesHelper.saveArrayList(
                    this,
                    Const.PRIVATE_MESSAGE_IDS,
                    privateThreadIDList
                )
            }
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
        val (name, photoUri) = getContactInfo(context, rawAddress)

        return ChatUser(
            threadId = threadId,
            latestMessage = latestMessage ?: "",
            timestamp = latestTimestamp,
            address = rawAddress,
            contactName = name,
            photoUri = photoUri,
            unreadCount = unreadCount,
            simSlot = simSlot
        )
    }
}