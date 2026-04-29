package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.AllContactsAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityNewConversationBinding
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.listener.OnClickContactInterface
import com.texting.sms.messaging_app.model.ContactModel
import com.texting.sms.messaging_app.utils.ContactNumberCache
import com.texting.sms.messaging_app.utils.LocaleHelper
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import com.vanniktech.ui.hideKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewConversationActivity : AppCompatActivity(), OnClickContactInterface, NetworkAvailableListener {
    private lateinit var binding: ActivityNewConversationBinding
    private lateinit var rvAllContactListAdapter: AllContactsAdapter
    private var allContactsList: MutableList<ContactModel> = mutableListOf()
    private var isScheduledMessage = false
    private var searchContactsJob: Job? = null

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
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }
        super.onCreate(savedInstanceState)
        networkUtil = NetworkConnectionUtil(this)
        networkUtil.setListener(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_conversation)
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

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@NewConversationActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@NewConversationActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@NewConversationActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@NewConversationActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("NewConversationActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@NewConversationActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@NewConversationActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@NewConversationActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
        if (intent.hasExtra(Const.FROM_PAGE)) {
            if (intent.getStringExtra(Const.FROM_PAGE) == Const.SCHEDULED_MESSAGE) {
                isScheduledMessage = true
            }
        }

        /**  Initialize an adapter of contact list  **/
        val isMobileNumbersOnly =
            SharedPreferencesHelper.getBoolean(this, Const.IS_MOBILE_NUMBERS_ONLY, true)
        val layoutManager = LinearLayoutManager(this)
        binding.rvAllContactsList.layoutManager = layoutManager.apply {
            isItemPrefetchEnabled = false
            initialPrefetchItemCount = 0
        }

        rvAllContactListAdapter = AllContactsAdapter(
            this,
            isMobileNumbersOnly
        ).apply {
            setHasStableIds(true)
        }
        binding.rvAllContactsList.adapter = rvAllContactListAdapter
        binding.rvAllContactsList.setHasFixedSize(true)

        (binding.rvAllContactsList.itemAnimator as? SimpleItemAnimator)
            ?.supportsChangeAnimations = false

        val isContactPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (isContactPermission) {
            CoroutineScope(Dispatchers.IO).launch {
                allContactsList =
                    getGroupedContacts(this@NewConversationActivity)

                withContext(Dispatchers.Main) {
                    if (allContactsList.isNotEmpty()) {
                        binding.rvNoFavouriteContacts.visibility = View.GONE
                        binding.paginationProgress.visibility = View.GONE
                        binding.rvAllContactsList.fadeIn()
                        rvAllContactListAdapter.submitList(allContactsList)

                        binding.etSearchContacts.requestFocus()
                        binding.etSearchContacts.post {
                            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showSoftInput(
                                binding.etSearchContacts,
                                InputMethodManager.SHOW_IMPLICIT
                            )
                        }
                    } else {
                        binding.paginationProgress.visibility = View.GONE
                        binding.rvAllContactsList.visibility = View.GONE
                        binding.rvNoFavouriteContacts.fadeIn()
                    }
                }
            }
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

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    startActivity(Intent(this@NewConversationActivity, HomeActivity::class.java))
                } else {
                    finish()
                }
            }
        })

        binding.etSearchContacts.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchContactsJob?.cancel()

                searchContactsJob = lifecycleScope.launch {
                    delay(150)

                    val query = s?.toString()?.trim().orEmpty()

                    withContext(Dispatchers.Main) {
                        binding.ivClose.visibility =
                            if (query.isNotEmpty()) View.VISIBLE else View.GONE
                    }

                    if (query.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            binding.rvUnknownNumber.visibility = View.GONE

                            if (allContactsList.isNotEmpty()) {
                                binding.rvNoFavouriteContacts.visibility = View.GONE
                                binding.rvAllContactsList.fadeIn()
                                rvAllContactListAdapter.submitList(allContactsList)
                            } else {
                                binding.rvAllContactsList.visibility = View.GONE
                                binding.rvNoFavouriteContacts.fadeIn()
                            }
                        }
                        return@launch
                    }

                    if (query.isDigitsOnly()) {
                        withContext(Dispatchers.Main) {
                            val finalQuery = "Send to $query"
                            binding.txtContactName.text = finalQuery
                            binding.txtContactNumber.text = query
                            binding.rvAllContactsList.visibility = View.GONE
                            binding.rvNoFavouriteContacts.visibility = View.GONE
                            binding.rvUnknownNumber.fadeIn()
                        }

                        filterContactsAsync(query, true)
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.rvUnknownNumber.visibility = View.GONE
                        }
                        filterContactsAsync(query)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.ivClose.setOnClickListener {
            binding.etSearchContacts.text.clear()
        }

        binding.rvUnknownNumber.setOnClickListener {
            hideKeyboard()

            binding.txtContactNumber.text.toString().let {
                if (it.isEmpty()) return@let

                val threadId = getThreadIdFromPhoneNumber(this, it)
                val intent = Intent(this, PersonalChatActivity::class.java)
                intent.putExtra(Const.THREAD_ID, threadId)
                intent.putExtra(Const.CONTACT_NUMBER, it)
                intent.putExtra(Const.FROM_PAGE, isScheduledMessage)
                startActivity(intent)
            }
        }
    }

    private fun filterContactsAsync(
        query: String,
        withNumberSearch: Boolean = false
    ) {
        lifecycleScope.launch(Dispatchers.Default) {
            val filtered = filterContactsInternal(query)

            withContext(Dispatchers.Main) {
                if (filtered.isNotEmpty()) {
                    binding.rvNoFavouriteContacts.visibility = View.GONE
                    binding.rvAllContactsList.visibility = View.VISIBLE
                    rvAllContactListAdapter.submitList(filtered)
                } else {
                    binding.rvAllContactsList.visibility = View.GONE
                    if (!withNumberSearch) {
                        binding.rvNoFavouriteContacts.fadeIn()
                    }
                }
            }
        }
    }

    private fun filterContactsInternal(query: String): List<ContactModel> {
        val lowerQuery = query.lowercase()
        val groupedMap = linkedMapOf<String, MutableList<ContactModel.ContactItem>>()
        var currentHeader: String? = null

        allContactsList.forEach { item ->
            when (item) {
                is ContactModel.HeaderWithFirstItem -> {
                    currentHeader = item.title

                    val match =
                        item.firstContact.name?.lowercase()?.contains(lowerQuery) == true ||
                                item.firstContact.phoneNumbers?.contains(lowerQuery) == true

                    if (match) {
                        groupedMap
                            .getOrPut(currentHeader) { mutableListOf() }
                            .add(item.firstContact)
                    }
                }

                is ContactModel.ContactItem -> {
                    val match =
                        item.name?.lowercase()?.contains(lowerQuery) == true ||
                                item.phoneNumbers?.contains(lowerQuery) == true

                    if (match) {
                        groupedMap
                            .getOrPut(currentHeader ?: "#") { mutableListOf() }
                            .add(item)
                    }
                }
            }
        }

        val result = mutableListOf<ContactModel>()

        groupedMap.forEach { (header, contacts) ->
            result.add(ContactModel.HeaderWithFirstItem(header, contacts.first()))
            if (contacts.size > 1) {
                result.addAll(contacts.drop(1))
            }
        }

        return result
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

    override fun onItemClick(contactsInfo: ContactModel.ContactItem) {
        hideKeyboard()
        contactsInfo.phoneNumbers?.let {
            if (it.isEmpty()) return@let

            val threadId = getThreadIdFromPhoneNumber(this, it)
            val intent = Intent(this, PersonalChatActivity::class.java)
            intent.putExtra(Const.THREAD_ID, threadId)
            intent.putExtra(Const.CONTACT_NUMBER, it)
            intent.putExtra(Const.FROM_PAGE, isScheduledMessage)
            startActivity(intent)
            finish()
        }
    }

    private fun getThreadIdFromPhoneNumber(context: Context, phoneNumber: String): Long? {
        val uri = "content://mms-sms/threadID".toUri()
            .buildUpon()
            .appendQueryParameter("recipient", phoneNumber)
            .build()

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow("_id"))
                return threadId
            }
        }
        return null
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