package com.texting.sms.messaging_app.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
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
import com.texting.sms.messaging_app.adapter.SearchContactsAdapter
import com.texting.sms.messaging_app.adapter.TopContactsAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivitySearchBinding
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.listener.OnSearchResultClickInterface
import com.texting.sms.messaging_app.model.ChatMatchResult
import com.texting.sms.messaging_app.model.ChatUser
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import com.texting.sms.messaging_app.utils.getDrawableFromAttr
import com.vanniktech.ui.hideKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class SearchActivity : BaseActivity(), OnChatUserInterface, OnSearchResultClickInterface, NetworkAvailableListener {
    private lateinit var binding: ActivitySearchBinding
    private lateinit var rvTopContactsAdapter: TopContactsAdapter
    private lateinit var rvSearchContactsAdapter: SearchContactsAdapter
    private lateinit var allConversationsList: List<ChatUser>
    private var searchQuery: String = ""
    private var searchJob: Job? = null

    private var allMessageWithContactsNameList = emptyList<ChatUser>()

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@SearchActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@SearchActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@SearchActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@SearchActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("SearchActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SearchActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SearchActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SearchActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTopContactsView.setLayoutManager(layoutManagerMessage)
        rvTopContactsAdapter = TopContactsAdapter(
            mutableListOf(),
            this
        )
        binding.rvTopContactsView.adapter = rvTopContactsAdapter
        (binding.rvTopContactsView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        val layoutManagerMessageContact: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvSearchViewInContacts.setLayoutManager(layoutManagerMessageContact)
        rvSearchContactsAdapter = SearchContactsAdapter(
            mutableListOf(),
            this
        )
        binding.rvSearchViewInContacts.adapter = rvSearchContactsAdapter
        (binding.rvSearchViewInContacts.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        getTopContactsList()

        binding.etSearchContacts.requestFocus()
        binding.etSearchContacts.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                binding.etSearchContacts,
                InputMethodManager.SHOW_IMPLICIT
            )
        }

        binding.rvSearchViewInContacts.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }

        binding.rvSearchViewInAllSMS.layoutManager = object : LinearLayoutManager(this) {
            override fun canScrollVertically(): Boolean {
                return false
            }
        }
    }

    private fun getTopContactsList() {
        lifecycleScope.launch(Dispatchers.IO) {
            val contactsMap = getAllContactsMap(this@SearchActivity)

            allConversationsList = getAllSmsThreads(this@SearchActivity)

            allConversationsList.forEach { item ->
                if (item.contactName.isNullOrEmpty()) {
                    item.contactName = contactsMap[item.address] ?: item.address
                }
            }

            val finalChatsList = SharedPreferencesHelper.filterNonPrivateThreads(
                this@SearchActivity,
                allConversationsList
            )

            withContext(Dispatchers.Main) {
                allMessageWithContactsNameList = finalChatsList.toMutableList()
                rvTopContactsAdapter.updateData(allMessageWithContactsNameList)
            }
        }
    }

    private fun getAllContactsMap(context: Context): Map<String, String> {
        val map = mutableMapOf<String, String>()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val number = cursor.getString(numberIndex)
                val name = cursor.getString(nameIndex)

                map[number] = name
            }
        }

        return map
    }

    data class SmsPart(
        val address: String,
        val body: String,
        val date: Long,
        val isRead: Boolean,
        val simSlot: Int,
        val type: Int
    )

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

        if (cursor == null || cursor.count == 0) return emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val threadIdIndex = it.getColumnIndex("thread_id")
                if (threadIdIndex == -1) continue

                val threadId = it.getLong(threadIdIndex)

                val address = it.getColumnIndex("address").let { idx ->
                    if (idx != -1) it.getString(idx) ?: "" else ""
                }

                val body = it.getColumnIndex("body").let { idx ->
                    if (idx != -1) it.getString(idx) ?: "" else ""
                }

                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                val isRead = it.getInt(it.getColumnIndexOrThrow("read")) == 1
                val simSlot = it.getColumnIndex("sub_id").let { index ->
                    if (index != -1) it.getInt(index) else -1
                }

                val typeIndex = it.getColumnIndex("type")
                val type = if (typeIndex != -1) it.getInt(typeIndex) else 1

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
                if (prevTime == null || abs(prevTime - sms.date) <= 100) {
                    if (buffer.isEmpty()) latestTimestamp = sms.date
                    if (sms.body.isNotEmpty()) {
                        buffer.insert(0, sms.body)
                    }
                    prevTime = sms.date
                } else {
                    break
                }
            }

            if (sortedMessages.isEmpty()) continue

            val latestMessage = sortedMessages.first()

            latestMergedMessage = if (buffer.isNotEmpty()) buffer.toString() else "..."

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

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.etSearchContacts.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchQuery = s.toString()

                if (searchQuery.isEmpty()) {
                    rvSearchContactsAdapter.updateData(emptyList(), "")
                    return
                }

                searchJob = lifecycleScope.launch {
                    delay(300)

                    binding.progressBar.visibility = View.VISIBLE

                    val filterList = filterChatUsers(allConversationsList, searchQuery)

                    val finalChatsList = SharedPreferencesHelper
                        .filterNonPrivateThreads(this@SearchActivity, filterList)

                    val finalAllowedIds = SharedPreferencesHelper
                        .filterNonPrivateThreads(this@SearchActivity, finalChatsList)
                        .map { it.threadId }

                    val filteredMap = finalChatsList.filter { (threadId, _) ->
                        threadId in finalAllowedIds
                    }

                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.INVISIBLE
                        rvSearchContactsAdapter.updateData(filteredMap, searchQuery)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ivClose.setImageDrawable(
                    if (!s.isNullOrEmpty())
                        getDrawableFromAttr(R.attr.clearTextIcon)
                    else
                        getDrawableFromAttr(R.attr.searchIcon)
                )
            }
        })

        binding.ivClose.setOnClickListener {
            binding.etSearchContacts.text.clear()
        }
    }

    private fun filterChatUsers(
        originalList: List<ChatUser>,
        query: String
    ): List<ChatUser> {

        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        return originalList
            .asSequence()
            .filter { it.searchKey.contains(q) }
            .take(30)
            .toList()
    }

    override fun chatUserClick(userChatDetails: ChatUser) {
        val intent = Intent(this, PersonalChatActivity::class.java)
        intent.putExtra(Const.THREAD_ID, userChatDetails.threadId)
        intent.putExtra(Const.SENDER_ID, userChatDetails.address)
        startActivity(intent)
    }

    override fun onItemClick(chatDetails: ChatMatchResult) {
        val intent = Intent(this, PersonalChatActivity::class.java)
        intent.putExtra(Const.THREAD_ID, chatDetails.threadId)
        intent.putExtra(Const.SENDER_ID, chatDetails.address)
        intent.putExtra(Const.SEARCH_QUERY, searchQuery)
        startActivity(intent)
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }

    override fun onPause() {
        hideKeyboard()
        super.onPause()
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