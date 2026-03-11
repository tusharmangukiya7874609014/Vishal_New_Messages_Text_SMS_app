package com.texting.sms.messaging_app.activity

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.role.RoleManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Rect
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.provider.Telephony
import android.speech.RecognizerIntent
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateUtils
import android.util.Log
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.ak.KalendarView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.AllTranslateLanguageAdapter
import com.texting.sms.messaging_app.adapter.ClipboardListAdapter
import com.texting.sms.messaging_app.adapter.HorizontalQuickResponseAdapter
import com.texting.sms.messaging_app.adapter.PersonalChatAdapter
import com.texting.sms.messaging_app.adapter.SelectedAttachFileAdapter
import com.texting.sms.messaging_app.adapter.SelectedImagesAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.ScheduledSMSDatabaseHelper
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityPersonalChatBinding
import com.texting.sms.messaging_app.databinding.DialogChooseLanguageTransalateBinding
import com.texting.sms.messaging_app.databinding.DialogDeleteSpecificMessageBinding
import com.texting.sms.messaging_app.databinding.DialogSelectDatePickerBinding
import com.texting.sms.messaging_app.databinding.DialogSimCardBinding
import com.texting.sms.messaging_app.databinding.ItemDeliveryConfirmationsBinding
import com.texting.sms.messaging_app.listener.CallbackHolder
import com.texting.sms.messaging_app.listener.NetworkListener
import com.texting.sms.messaging_app.listener.OnClickPreviewImageInterface
import com.texting.sms.messaging_app.listener.OnClipboardClickInterface
import com.texting.sms.messaging_app.listener.OnHighlightView
import com.texting.sms.messaging_app.listener.OnOpenFullChatInterface
import com.texting.sms.messaging_app.listener.OnQuickMessageClickInterface
import com.texting.sms.messaging_app.listener.OnSelectedMessageFeatureClick
import com.texting.sms.messaging_app.listener.RemoveFileInterface
import com.texting.sms.messaging_app.listener.RemoveImageInterface
import com.texting.sms.messaging_app.listener.SelectLanguageInterface
import com.texting.sms.messaging_app.model.AttachFile
import com.texting.sms.messaging_app.model.ChatModel
import com.texting.sms.messaging_app.model.ContactInfo
import com.texting.sms.messaging_app.model.MatchPosition
import com.texting.sms.messaging_app.model.QuickResponse
import com.texting.sms.messaging_app.model.ScheduledSms
import com.texting.sms.messaging_app.receiver.NetworkReceiver
import com.texting.sms.messaging_app.receiver.SmsAlarmReceiver
import com.texting.sms.messaging_app.response.Language
import com.texting.sms.messaging_app.utils.ContactNameCache
import com.texting.sms.messaging_app.utils.LocaleHelper
import com.texting.sms.messaging_app.utils.StarCategory
import com.texting.sms.messaging_app.utils.getColorFromAttr
import com.texting.sms.messaging_app.utils.getDrawableFromAttr
import com.texting.sms.messaging_app.viewmodel.AllLanguagesViewModel
import com.texting.sms.messaging_app.viewmodel.MessageTranslateViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.EmojiTheming
import com.vanniktech.emoji.search.NoSearchEmoji
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class PersonalChatActivity : AppCompatActivity(), RemoveImageInterface,
    OnClickPreviewImageInterface, SelectLanguageInterface, OnClipboardClickInterface,
    RemoveFileInterface, NetworkListener, OnQuickMessageClickInterface,
    OnSelectedMessageFeatureClick, OnOpenFullChatInterface {
    private lateinit var binding: ActivityPersonalChatBinding
    private lateinit var rvPersonalChatListAdapter: PersonalChatAdapter
    private lateinit var rvSelectedImagesAdapter: SelectedImagesAdapter
    private lateinit var rvSelectedFilesAdapter: SelectedAttachFileAdapter
    private lateinit var rvClipboardAdapter: ClipboardListAdapter
    private lateinit var rvQuickResponseAdapter: HorizontalQuickResponseAdapter
    private lateinit var quickMessageResponseList: MutableList<QuickResponse>
    private lateinit var selectedImageUris: MutableList<Uri>
    private lateinit var selectedFile: MutableList<AttachFile>
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var contactUserDetails: ContactInfo? = null
    private var senderIdOrNumber: String? = null
    private var hasAutoScrolledOnKeyboard = false
    private var finalChatList = mutableListOf<ChatModel>()
    private var isSenderOrNumber = false
    private var threadId = 0L
    private lateinit var captureImageUri: Uri
    private var contactNumber: String = Const.STRING_DEFAULT_VALUE
    private lateinit var scheduledDate: Date
    private var isScheduledMessgae = false
    private var finalScheduledTime = Const.STRING_DEFAULT_VALUE
    private lateinit var emojiPopup: EmojiPopup
    private var wallpaperList = mutableListOf<Int>()
    private var chatBoxColorList = mutableListOf<Int>()
    private var allLanguagesList = mutableListOf<Language>()
    private var selectedLanguageCode = Const.STRING_DEFAULT_VALUE
    private lateinit var rvLanguageAdapter: AllTranslateLanguageAdapter
    private val viewModel: AllLanguagesViewModel by viewModels()
    private val messageTranslateViewModel: MessageTranslateViewModel by viewModels()
    private var positionSelected = 0
    private var locationClient: FusedLocationProviderClient? = null
    private var defaultClipboardList = mutableListOf<String>()
    private var funnyClipboardList = mutableListOf<String>()
    private var loveClipboardList = mutableListOf<String>()
    private var wishesClipboardList = mutableListOf<String>()
    private var sadClipboardList = mutableListOf<String>()
    private var friendClipboardList = mutableListOf<String>()
    private var birthdayClipboardList = mutableListOf<String>()
    private var lifeClipboardList = mutableListOf<String>()
    private var motivationalClipboardList = mutableListOf<String>()
    private var savageClipboardList = mutableListOf<String>()
    private var storeThreadIDList = ArrayList<String>()
    private var searchQuery = ""
    private var matchesList: List<MatchPosition> = emptyList()
    private var currentMatchIndex: Int = 0
    private var lastChatBoxStylePosition = 0
    private var isSubjectField = false
    private var latsChatBoxColorPosition = 0
    private var lastIsMeChatBoxColor = true
    private var isFirstTime = true
    private lateinit var networkReceiver: NetworkReceiver
    private var isNetworkReceiverRegistered = false
    private var isNetworkAvailable = true
    private var isAlreadyStar = false
    private lateinit var translateDialog: Dialog

    private val pickContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val contactUri: Uri? = result.data?.data
                contactUri?.let {
                    getContactInfo(it)
                }
            }
        }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                openGalleryForImages()
            }
        }

    private val editContactLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult

            val contactUri = data.data
            contactUri?.let {
                val contactName = getContactNameFromUri(this, it)
                val updateName = ContactNameCache.extractName(
                    this,
                    senderIdOrNumber.toString()
                )
                ContactNameCache.putName(senderIdOrNumber.toString(), updateName)
                binding.txtUserName.text = contactName.toString()
            }
        }

    private val selectImagesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult

        val data = result.data ?: return@registerForActivityResult

        lifecycleScope.launch {
            val newUris = withContext(Dispatchers.Default) {
                val list = ArrayList<Uri>()

                data.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) {
                        list.add(clip.getItemAt(i).uri)
                    }
                } ?: data.data?.let { uri ->
                    list.add(uri)
                }

                list
            }

            if (newUris.isEmpty()) return@launch

            selectedImageUris.addAll(newUris)

            rvSelectedImagesAdapter.updateData(selectedImageUris)

            if (binding.rvSelectedImages.visibility != View.VISIBLE) {
                binding.rvSelectedImages.fadeIn()
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            handleSelectedFiles(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = Color.WHITE
        }
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_personal_chat)
        networkReceiver = NetworkReceiver(this)
        scheduledDate = Date()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        translateDialog = Dialog(this)
        lastChatBoxStylePosition =
            SharedPreferencesHelper.getInt(this, Const.CHAT_BOX_STYLE_POSITION, 0)
        selectedImageUris = mutableListOf()
        selectedFile = mutableListOf()

        if (intent.hasExtra(Const.THREAD_ID)) {
            threadId = intent.getLongExtra(Const.THREAD_ID, 0)
        }

        receiverSMSOrMMS()
        initView()
        initObserver()
        initClipboard()
        initClickListener()
        runAdsCampion()
        CallbackHolder.highlightViewListener = object : OnHighlightView {
            override fun onItemClick(messagesId: String) {
                if (messagesId.startsWith("Images")) {
                    val mediaUri = extractIdFromLabel(messagesId)

                    val index = finalChatList.indexOfFirst {
                        it is ChatModel.MessageItem && it.mediaUri?.lastPathSegment.toString() == mediaUri
                    }

                    if (index != -1) {
                        val layoutManager =
                            binding.rvSenderChatView.layoutManager as LinearLayoutManager
                        val offset = binding.rvSenderChatView.height / 2
                        layoutManager.scrollToPositionWithOffset(index, offset)

                        binding.rvSenderChatView.postDelayed({
                            rvPersonalChatListAdapter.highlightMessageOfView(messagesId)
                        }, 200)
                    }
                } else {
                    val index = finalChatList.indexOfFirst {
                        it is ChatModel.MessageItem && it.smsId.toString() == messagesId
                    }

                    if (index != -1) {
                        val layoutManager =
                            binding.rvSenderChatView.layoutManager as LinearLayoutManager
                        val offset = binding.rvSenderChatView.height / 2
                        layoutManager.scrollToPositionWithOffset(index, offset)

                        binding.rvSenderChatView.postDelayed({
                            rvPersonalChatListAdapter.highlightMessageOfView(messagesId)
                        }, 200)
                    }
                }
            }
        }
    }

    private fun extractIdFromLabel(label: String): String {
        return if (label.contains(":")) {
            label.substringAfter(":").trim()
        } else {
            label
        }
    }

    override fun onStart() {
        registerNetworkReceiver()
        super.onStart()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@PersonalChatActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@PersonalChatActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@PersonalChatActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@PersonalChatActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@PersonalChatActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("PersonalChatActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@PersonalChatActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@PersonalChatActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@PersonalChatActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val bannerAdsType = getAdsPageResponse.optString("bannerAdsType").ifEmpty {
                "adaptive"
            } ?: "adaptive"

            Log.d(
                "ABCD",
                "Out isPrivateChatsAdsVisible :- ${getAdsPageResponse.optBoolean("isPrivateChatsAdsVisible")}"
            )
            Log.d(
                "ABCD",
                "Out isDefaultSMSChatsAdsVisible :- ${getAdsPageResponse.optBoolean("isDefaultSMSChatsAdsVisible")}"
            )

            val isPrivateChatsAdsVisible = getAdsPageResponse.optBoolean("isPrivateChatsAdsVisible")

            val isDefaultSMSChatsAdsVisible =
                getAdsPageResponse.optBoolean("isDefaultSMSChatsAdsVisible")

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                Log.d(
                    "ABCD",
                    "IN isPrivateChatsAdsVisible:- ${getAdsPageResponse.optBoolean("isPrivateChatsAdsVisible")}"
                )
                Log.d(
                    "ABCD",
                    "IN isDefaultSMSChatsAdsVisible :- ${getAdsPageResponse.optBoolean("isDefaultSMSChatsAdsVisible")}"
                )

                if (binding.rvSenderNotSupport.isVisible && isDefaultSMSChatsAdsVisible) {
                    if (isCurrentPageNativeAdsEnabled && !isCurrentPageBannerAdsEnabled) {
                        runNativeAds(
                            nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
                        )
                    } else if (isCurrentPageBannerAdsEnabled && !isCurrentPageNativeAdsEnabled) {
                        runBannerAds(
                            bannerAdsId = currentPageBannerAdsID, bannerAdsType = bannerAdsType
                        )
                    }
                } else {
                    if (isPrivateChatsAdsVisible && !binding.rvSenderNotSupport.isVisible) {
                        if (isCurrentPageNativeAdsEnabled && !isCurrentPageBannerAdsEnabled) {
                            runNativeAds(
                                nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
                            )
                        } else if (isCurrentPageBannerAdsEnabled && !isCurrentPageNativeAdsEnabled) {
                            runBannerAds(
                                bannerAdsId = currentPageBannerAdsID, bannerAdsType = bannerAdsType
                            )
                        }
                    }
                }

                if (isAppInterstitialAdsEnabled) {
                    InterstitialAdHelper.apply {
                        loadAd(this@PersonalChatActivity)
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

    private fun userChatDetails() {
        if (!intent.hasExtra(Const.THREAD_ID)) return

        val hasSenderId = intent.hasExtra(Const.SENDER_ID)
        val hasContactNumber = intent.hasExtra(Const.CONTACT_NUMBER)

        if (hasSenderId) {
            isSenderOrNumber = true
            senderIdOrNumber = intent.getStringExtra(Const.SENDER_ID)
        } else if (hasContactNumber) {
            isSenderOrNumber = false
            senderIdOrNumber = intent.getStringExtra(Const.CONTACT_NUMBER)
            contactNumber = senderIdOrNumber.toString()
        }

        if (hasSenderId) {
            isSenderOrNumber = true
            senderIdOrNumber = intent.getStringExtra(Const.SENDER_ID)
            contactUserDetails = senderIdOrNumber?.let { getContactInfoFromSenderId(this, it) }

            if (contactUserDetails?.number?.replace("+", "")?.replace(" ", "")
                    ?.isDigitsOnly() == true && !contactUserDetails?.number.contentEquals("null")
            ) {
                contactNumber = contactUserDetails?.number.toString()
                binding.rvQuickResponseList.visibility = View.GONE
                binding.rvSenderNotSupport.visibility = View.GONE
            } else {
                if (senderIdOrNumber?.replace("+", "")?.replace(" ", "")?.isDigitsOnly() == true) {
                    contactNumber = senderIdOrNumber.toString()
                    binding.rvQuickResponseList.visibility = View.GONE
                    binding.rvSenderNotSupport.visibility = View.GONE
                } else {
                    binding.ivAttach.visibility = View.GONE
                    binding.rvSendMessage.visibility = View.GONE
                    binding.rvMessageView.visibility = View.GONE
                    binding.rvQuickResponseList.visibility = View.GONE
                    binding.rvSenderNotSupport.visibility = View.VISIBLE
                }
            }

            if (intent.hasExtra(Const.SEARCH_QUERY)) {
                searchQuery = intent.getStringExtra(Const.SEARCH_QUERY).toString()
                binding.rvChatDetails.visibility = View.GONE
                binding.txtSearchText.text = searchQuery
                binding.rvSearchView.fadeIn()
            } else {
                binding.rvSearchView.visibility = View.GONE
            }
        } else {
            if (intent.hasExtra(Const.CONTACT_NUMBER)) {
                binding.rvSenderNotSupport.visibility = View.GONE
                binding.rvSearchView.visibility = View.GONE
                binding.rvQuickResponseList.visibility = View.GONE
                isSenderOrNumber = false
                senderIdOrNumber = intent.getStringExtra(Const.CONTACT_NUMBER)
                contactNumber = senderIdOrNumber.toString()
                contactUserDetails =
                    senderIdOrNumber?.let { getContactInfoFromPhoneNumber(this, it) }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val originalMessages: List<ChatModel.MessageItem> =
                getMessagesForThread(this@PersonalChatActivity, threadId)
            finalChatList = groupMessagesWithHeaders(originalMessages)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                if (finalChatList.isNotEmpty()) {
                    binding.txtHint.visibility = View.GONE
                    if (searchQuery.isNotEmpty()) {
                        onSearch(searchQuery)
                    } else {
                        rvPersonalChatListAdapter.updateData(finalChatList, searchQuery)
                        if (isFirstTime) {
                            scrollToUnreadOrLatest(finalChatList)
                            isFirstTime = false
                        }
                    }
                } else {
                    binding.txtHint.visibility = View.VISIBLE
                    rvPersonalChatListAdapter.updateData(finalChatList, searchQuery)
                }
            }
        }

        if (contactUserDetails?.name != null && !contactUserDetails?.name.toString()
                .contentEquals("null")
        ) {
            binding.txtUserName.text = contactUserDetails?.name.toString()
        } else {
            binding.txtUserName.text = senderIdOrNumber
        }

        if (isAddressBlocked(this, senderIdOrNumber.toString())) {
            binding.cvProfileView.setCardBackgroundColor(getColor(R.color.blocked_user_profile))
            binding.ivOriginalProfile.visibility = View.GONE
            binding.ivDefaultProfile.visibility = View.GONE
            binding.ivBlockProfile.visibility = View.VISIBLE
            rvPersonalChatListAdapter.updateProfileView(true, "")
        } else {
            if (!contactUserDetails?.photoUri.contentEquals(
                    "null"
                ) && contactUserDetails?.photoUri != null
            ) {
                binding.ivDefaultProfile.visibility = View.GONE
                binding.ivBlockProfile.visibility = View.GONE
                binding.ivOriginalProfile.visibility = View.VISIBLE
                Glide.with(this).load(contactUserDetails?.photoUri?.toUri())
                    .into(binding.ivOriginalProfile)
                rvPersonalChatListAdapter.updateProfileView(
                    false, contactUserDetails?.photoUri.toString()
                )
            } else {
                if (SharedPreferencesHelper.getBoolean(
                        this, Const.IS_CHANGE_PROFILE_COLOR, false
                    )
                ) {
                    binding.ivDefaultProfile.setImageDrawable(
                        ContextCompat.getDrawable(
                            this, R.drawable.ic_profile
                        )
                    )
                } else {
                    binding.ivDefaultProfile.setImageDrawable(
                        ContextCompat.getDrawable(
                            this, R.drawable.ic_dark_profile_popup
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
                rvPersonalChatListAdapter.updateProfileView(false, "null")
            }
        }

        if (intent.hasExtra(Const.FROM_PAGE)) {
            if (intent.getBooleanExtra(Const.FROM_PAGE, false)) {
                if (binding.rvScheduledView.isGone) {
                    showScheduledTimeDialog()
                }
            }
        }

        val address = getAddressFromThreadId(this, threadId)
        if (address != null) {
            val contactId = getContactIdFromPhoneNumber(this, address)
            if (contactId != null) {
                binding.ivAddToContacts.setImageDrawable(getDrawableFromAttr(R.attr.contactEditIcon))
            } else {
                binding.ivAddToContacts.setImageDrawable(getDrawableFromAttr(R.attr.addToContact))
            }
        } else {
            val contactId = getContactIdFromPhoneNumber(this, contactNumber)
            if (contactId != null) {
                binding.ivAddToContacts.setImageDrawable(getDrawableFromAttr(R.attr.contactEditIcon))
            } else {
                binding.ivAddToContacts.setImageDrawable(getDrawableFromAttr(R.attr.addToContact))
            }
        }
    }

    private fun initClipboard() {
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                defaultClipboardList = arrayListOf(
                    getString(R.string.in_a_meeting_will_call_you_back_later),
                    getString(R.string.please_call_me_later_thanks),
                    getString(R.string.have_a_nice_day),
                    getString(R.string.ok_got_it),
                    getString(R.string.on_the_way_will_be_there_soon),
                    getString(R.string.i_am_fine_do_not_worry),
                    getString(R.string.thank_you),
                    getString(R.string.you_are_welcome),
                    getString(R.string.wish_you_the_best),
                    getString(R.string.come_here_now)
                )

                funnyClipboardList = arrayListOf(
                    getString(R.string.i_need_6_months_of_vacation_twice_a_year),
                    getString(R.string.i_love_my_job_only_when_i_am_on_vacation),
                    getString(R.string.i_am_not_lazy_i_am_just_on_my_energy_saving_mode),
                    getString(R.string._404_motivation_not_found),
                    getString(R.string.there_is_no_sunrise_so_beautiful_that_it_is_worth_waking_me_up_to_see_it),
                    getString(R.string.i_am_not_completely_useless_i_can_be_used_as_a_bad_example),
                    getString(R.string.if_people_could_read_my_mind_i_had_get_punched_in_the_face_a_lot),
                    getString(R.string.don_t_worry_i_ve_got_jokes_for_days_just_none_today),
                    getString(R.string.someday_you_will_go_far_and_i_hope_you_stay_there),
                    getString(R.string.not_wearing_glasses_anymore_i_have_seen_enough),
                    getString(R.string.mentally_at_the_beach),
                    getString(R.string.i_do_not_understand_your_specific_kind_of_crazy_but_i_do_admire_your_total_commitment_to_it),
                    getString(R.string.a_day_without_sunshine_is_like_you_know_night),
                    getString(R.string.ctrl_alt_delete_my_responsibilities)
                )

                loveClipboardList = arrayListOf(
                    getString(R.string.just_thinking_about_you_puts_a_smile_on_my_face),
                    getString(R.string.you_re_the_peanut_butter_to_my_jelly),
                    getString(R.string.my_day_seems_incomplete_without_you),
                    getString(R.string.the_most_important_thing_in_this_world_is_family_and_love),
                    getString(R.string.you_can_not_blame_gravity_for_falling_in_love),
                    getString(R.string.you_re_the_reason_i_check_my_phone),
                    getString(R.string.you_are_the_most_beautiful_person_both_inside_and_out_i_am_so_lucky_to_have_you),
                    getString(R.string.a_million_thanks_to_the_most_amazing_person_i_know),
                    getString(R.string.you_re_my_favorite_notification),
                    getString(R.string.there_are_no_words_i_could_use_to_describe_the_love_we_share_i_am_so_incredibly_thankful_for_you),
                    getString(R.string.love_is_when_you_have_options_to_leave_but_you_decide_to_stay),
                    getString(R.string.i_m_yours_no_refunds),
                    getString(R.string.i_like_when_you_smile_but_i_love_it_when_i_am_the_reason),
                    getString(R.string.love_understands_love_it_needs_no_talk)
                )

                wishesClipboardList = arrayListOf(
                    getString(R.string.cheers_to_another_year_of_greatness),
                    getString(R.string.wishing_you_strength_and_focus_to_achieve_your_goals),
                    getString(R.string.all_the_best_i_wish_you_all_best_for_a_prosperous_future_ahead),
                    getString(R.string.happy_birthday_may_all_your_dreams_come_true),
                    getString(R.string.may_today_bring_calm_and_clarity),
                    getString(R.string.merry_christmas_spread_love_everywhere_you_go),
                    getString(R.string.the_most_important_thing_is_to_enjoy_your_life_to_be_happy_it_is_all_that_matters),
                    getString(R.string.wishing_you_a_day_full_of_smiles_and_sunshine),
                    getString(R.string.sometimes_when_things_are_falling_apart_they_may_actually_be_falling_into_place),
                    getString(R.string.may_your_hard_work_bring_you_success_and_satisfaction),
                    getString(R.string.you_are_braver_then_you_believe_and_stronger_then_you_seem_and_smarter_then_you_think),
                    getString(R.string.sending_you_best_wishes_for_an_amazing_journey_ahead),
                    getString(R.string.happy_birthday_wishing_you_joy_and_success_always),
                    getString(R.string.congratulations_cheers_to_your_success_and_a_bright_future)
                )

                sadClipboardList = arrayListOf(
                    getString(R.string.trust_is_hard_especially_after_it_s_broken),
                    getString(R.string.givers_need_to_set_limits_because_takers_rarely_do),
                    getString(R.string.don_t_cling_to_mistake_just_because_you_spent_a_long_time_making_it),
                    getString(R.string.i_m_fine_just_tired_of_everything),
                    getString(R.string.i_realise_now_that_the_person_i_used_to_be_is_not_the_person),
                    getString(R.string.sadness_is_not_the_opposite_of_happiness_it_s_a_part_of_it),
                    getString(R.string.the_worst_kind_of_pain_is_when_your_heart_cry_and_your_eyes_are_dry),
                    getString(R.string.time_can_t_heal_your_emotional_pain_if_you_don_t_learn_how_to_let_go),
                    getString(R.string.they_say_follow_your_heart_but_your_heart_is_a_million_pieces_which_piece_do_you_follow),
                    getString(R.string.sometimes_silence_is_the_loudest_cry),
                    getString(R.string.if_there_is_anything_that_is_true_in_your_life_it_is_the_present_passing_moment_sadness_is_part_of_it),
                    getString(R.string.another_chapter_has_to_end_before_a_new_one_can_begin),
                    getString(R.string.smiling_on_the_outside_broken_on_the_inside),
                    getString(R.string.behind_every_sweet_smile_there_is_a_bitter_sadness_that_no_one_can_ever_see_and_feel)
                )

                friendClipboardList = arrayListOf(
                    getString(R.string.best_friends_are_like_stars_always_there_even_if_you_can_t_see_them),
                    getString(R.string.a_best_friend_is_like_a_four_leaf_clover_hard_to_find_lucky_to_have),
                    getString(R.string.a_friend_eye_is_a_good_mirror),
                    getString(R.string.you_re_not_just_a_friend_you_re_family),
                    getString(R.string.friends_are_god_s_way_of_taking_care_of_us),
                    getString(R.string.you_smile_i_smile_you_get_hurt_i_get_hurt_you_cry_i_cry_you_jump_off_a_bridge_i_am_gonna_miss_you_buddy),
                    getString(R.string.true_friends_are_like_diamonds_pure_and_rare_fake_friends_are_like_leaves_found_everywhere),
                    getString(R.string.no_distance_can_break_real_friendship),
                    getString(R.string.true_friend_will_open_your_eyes_mind_and_heart_to_a_better_more_fulfilling_life),
                    getString(R.string.a_friend_is_someone_who_understand_your_past_believes_in_your_future_abd_accepts_you_today_just_the_way_you_are),
                    getString(R.string.real_friends_don_t_need_daily_talks_just_real_connection),
                    getString(R.string.to_have_true_friends_you_must_be_a_true_friends),
                    getString(R.string.you_always_know_how_to_make_me_smile),
                    getString(R.string.friendship_is_like_violin_music_may_stop_now_or_then_but_strings_may_be_together_forever)
                )

                birthdayClipboardList = arrayListOf(
                    getString(R.string.happy_birthday_wishing_you_all_the_happiness_in_the_world),
                    getString(R.string.may_your_birthday_be_full_of_joy_love_and_celebration_you_deserve_the_very_best),
                    getString(R.string.hoping_all_your_birthday_wishes_come_true),
                    getString(R.string.i_can_not_think_of_a_better_gift_than_your_friendship_happy_birthday),
                    getString(R.string.cheers_to_another_trip_around_the_sun),
                    getString(R.string.another_year_older_wiser_and_more_fabulous),
                    getString(R.string.forget_the_past_look_forward_to_the_future_for_the_best_things_are_yet_to_come),
                    getString(R.string.enjoy_your_special_day_to_the_fullest),
                    getString(R.string.you_re_older_today_than_yesterday_but_younger_than_tomorrow_happy_birthday),
                    getString(R.string.happy_birthday_to_someone_who_is_forever_young),
                    getString(R.string.keep_shining_birthday_star),
                    getString(R.string.a_simple_celebration_a_gathering_of_friends_here_wishing_you_great_happiness_and_a_joy_that_never_ends),
                    getString(R.string.best_wishes_on_your_birthday_may_you_have_maximum_fun_today_and_minimum_hangover_tomorrow),
                    getString(R.string.wishing_you_a_year_filled_with_love_and_joy)
                )

                lifeClipboardList = arrayListOf(
                    getString(R.string.grow_through_what_you_go_through),
                    getString(R.string.a_journey_of_a_thousand_miles_begins_with_a_single_step),
                    getString(R.string.life_begins_at_the_end_of_your_comfort_zone),
                    getString(R.string.the_great_pleasure_in_life_is_doing_what_people_say_you_cannot_do),
                    getString(R.string.create_a_life_you_don_t_need_a_vacation_from),
                    getString(R.string.you_can_not_have_better_tomorrow_if_you_are_thinking_about_yesterday_all_the_time),
                    getString(R.string.happiness_is_not_a_goal_but_a_wat_of_life),
                    getString(R.string.be_careful_with_your_words_once_they_are_said_they_can_only_be_forgiven_not_forgotten),
                    getString(R.string.doubt_kills_more_dreams_than_failure_ever_will),
                    getString(R.string.if_you_do_not_live_for_something_you_will_die_for_nothing),
                    getString(R.string.success_starts_with_self_belief),
                    getString(R.string.recognize_the_small_things_they_are_usually_means_the_most),
                    getString(R.string.to_forgive_takes_strength_to_forget_takes_foolishness),
                    getString(R.string.fall_seven_times_stand_up_eight)
                )

                motivationalClipboardList = arrayListOf(
                    getString(R.string.believe_in_yourself_always),
                    getString(R.string.one_day_or_day_one_you_decide),
                    getString(R.string.nothing_is_impossible),
                    getString(R.string.don_t_stop_until_you_re_proud),
                    getString(R.string.it_is_never_too_late_to_be_what_you_might_have_been),
                    getString(R.string.you_have_to_believe_in_yourself_when_no_one_else_does),
                    getString(R.string.progress_not_perfection),
                    getString(R.string.work_hard_push_yourself_because_on_one_else_is_going_to_do_it_for_you),
                    getString(R.string.life_has_got_all_those_twists_and_turns_you_have_got_to_hold_on_tight_and_off_you_go),
                    getString(R.string.work_hard_in_silence_let_success_speak),
                    getString(R.string.keep_your_face_always_toward_the_sunshine_and_shadows_fill_fall_behind_you),
                    getString(R.string.success_is_not_final_failure_is_not_fatal_it_is_the_courage_to_continue_that_counts),
                    getString(R.string.keep_going_you_re_getting_there),
                    getString(R.string.do_not_allow_people_to_dim_your_shine_because_they_are_blinded_tell_them_to_put_some_sunglasses_on)
                )

                savageClipboardList = arrayListOf(
                    getString(R.string.no_filter_no_fear_just_facts),
                    getString(R.string.i_m_not_heartless_i_just_learned_how_to_use_mine_wisely),
                    getString(R.string.silent_but_deadly_like_my_comebacks),
                    getString(R.string.take_me_as_i_am_or_watch_me_as_i_go),
                    getString(R.string.the_best_thing_about_being_me_i_am_limited_edition_there_are_no_other_copies),
                    getString(R.string.the_only_difference_between_me_and_madman_is_that_i_am_not_mad),
                    getString(R.string.if_you_think_i_am_bad_then_come_to_me_i_will_give_you_more_better_reason_to_hate_me),
                    getString(R.string.life_is_runway_and_i_am_the_damn_supermodel),
                    getString(R.string.i_am_too_busy_being_a_boss),
                    getString(R.string.they_say_good_things_take_time_that_is_why_i_am_always_late),
                    getString(R.string.life_is_short_smile_while_you_still_have_teeth),
                    getString(R.string.do_not_study_me_you_won_t_graduate),
                    getString(R.string.no_makeup_no_problem_unfiltered_and_unapologetic),
                    getString(R.string.no_gps_is_needed_i_am_on_my_own_path)
                )
            }

            if (binding.iClipBoardView.rvSuggestedClipboardList.adapter == null) {
                binding.iClipBoardView.rvSuggestedClipboardList.apply {
                    layoutManager = LinearLayoutManager(this@PersonalChatActivity)
                    itemAnimator = null
                    setHasFixedSize(true)
                }

                rvClipboardAdapter =
                    ClipboardListAdapter(mutableListOf(), this@PersonalChatActivity)
                binding.iClipBoardView.rvSuggestedClipboardList.adapter = rvClipboardAdapter
            }

            rvClipboardAdapter.updateData(defaultClipboardList)
        }
    }

    private fun initObserver() {
        viewModel.allLanguages.observe(this) {
            if (it.isNotEmpty()) {
                allLanguagesList.clear()
                allLanguagesList.addAll(it)
            }
        }

        messageTranslateViewModel.translateMessage.observe(this) {
            if (it.status) {
                rvPersonalChatListAdapter.updateSpecificItem(positionSelected, it.response)
            }
        }
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

    private fun receiverSMSOrMMS() {
        contentResolver.registerContentObserver(
            "content://sms".toUri(),
            true,
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)

                    CoroutineScope(Dispatchers.IO).launch {
                        val cursor = contentResolver.query(
                            "content://sms".toUri(),
                            arrayOf("address"),
                            null,
                            null,
                            "date DESC LIMIT 1"
                        )

                        var latestSender: String? = null
                        cursor?.use {
                            if (it.moveToFirst()) {
                                latestSender = it.getString(it.getColumnIndexOrThrow("address"))
                            }
                        }

                        // Normalize and compare sender number with currentChatAddress
                        val normalizedLatest =
                            latestSender?.replace("+91", "")?.filter { it.isDigit() }
                        val normalizedCurrent =
                            senderIdOrNumber?.replace("+91", "")?.filter { it.isDigit() }

                        if (normalizedLatest != null && normalizedLatest == normalizedCurrent) {
                            val originalMessages: List<ChatModel.MessageItem> =
                                getMessagesForThread(this@PersonalChatActivity, threadId)
                            finalChatList = groupMessagesWithHeaders(originalMessages)

                            withContext(Dispatchers.Main) {
                                if (finalChatList.isNotEmpty()) {
                                    binding.txtHint.visibility = View.GONE
                                    rvPersonalChatListAdapter.updateData(finalChatList, searchQuery)
                                    scrollToUnreadOrLatest(finalChatList)
                                }
                            }
                        }
                    }
                }
            })
    }

    private fun initView() {
        storeThreadIDList.clear()
        SharedPreferencesHelper.saveArrayList(
            this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
        )

        viewModel.loadAllLanguages()
        listOfChatBoxColor()

        quickMessageResponseList = SharedPreferencesHelper.getQuickMessageList(this).toMutableList()
        if (quickMessageResponseList.isEmpty()) {
            quickMessageResponseList = listOf(
                QuickResponse("❤\uFE0F"),
                QuickResponse("\uD83D\uDE00"),
                QuickResponse("\uD83D\uDC4D"),
                QuickResponse("\uD83D\uDE4F"),
                QuickResponse("\uD83D\uDE0E"),
                QuickResponse("\uD83D\uDE02"),
                QuickResponse("Hi \uD83D\uDC4B"),
                QuickResponse("How are you?"),
                QuickResponse("Okay \uD83D\uDC4D"),
                QuickResponse("Thanks \uD83D\uDE4F"),
            ).toMutableList()
            SharedPreferencesHelper.saveQuickMessageList(this, quickMessageResponseList)
        }

        val (simList, _) = getSimDetails(this)
        if (simList.isNotEmpty()) {
            if (simList[0].subscriptionId == SharedPreferencesHelper.getInt(
                    this, Const.SIM_SLOT_NUMBER, SubscriptionManager.getDefaultSmsSubscriptionId()
                )
            ) {
                binding.ivSimCard.setImageDrawable(
                    ContextCompat.getDrawable(
                        this, R.drawable.ic_sim_one
                    )
                )
            } else {
                binding.ivSimCard.setImageDrawable(
                    ContextCompat.getDrawable(
                        this, R.drawable.ic_sim_2
                    )
                )
            }
        }

        emojiPopup = EmojiPopup(
            binding.main, binding.etWriteMessage, theming = EmojiTheming(
                backgroundColor = getColorFromAttr(R.attr.mainBackground),
                primaryColor = getColorFromAttr(R.attr.subTextColor),
                secondaryColor = getColor(R.color.app_theme_color),
                dividerColor = Color.GRAY,
                textColor = Color.WHITE,
                textSecondaryColor = Color.GRAY,
            ), searchEmoji = NoSearchEmoji
        )

        var chatBoxColor =
            if (isDarkTheme()) R.color.dark_chat_default_color else R.color.light_chat_default_color
        latsChatBoxColorPosition =
            SharedPreferencesHelper.getInt(this, Const.CHAT_BOX_COLOR_POSITION, -1)

        if (latsChatBoxColorPosition != -1) {
            if (latsChatBoxColorPosition in chatBoxColorList.indices) {
                chatBoxColor = chatBoxColorList[latsChatBoxColorPosition]
                lastIsMeChatBoxColor =
                    SharedPreferencesHelper.getBoolean(this, Const.IS_ME_CHAT_BOX_COLOR, true)
            }
        } else {
            chatBoxColor =
                if (isDarkTheme()) R.color.dark_chat_default_color else R.color.light_chat_default_color
            lastIsMeChatBoxColor = true
        }

        val layoutManagerMessage: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvSenderChatView.setLayoutManager(layoutManagerMessage)
        rvPersonalChatListAdapter = PersonalChatAdapter(
            this,
            threadId,
            mutableListOf(),
            chatBoxColor,
            lastIsMeChatBoxColor,
            storeThreadIDList,
            this,
            this,
            onOpenFullChatInterface = this
        )

        binding.rvSenderChatView.adapter = rvPersonalChatListAdapter
        (binding.rvSenderChatView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        val layoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedImages.setLayoutManager(layoutManager)
        rvSelectedImagesAdapter = SelectedImagesAdapter(mutableListOf(), this, this)
        binding.rvSelectedImages.adapter = rvSelectedImagesAdapter

        val layoutManagerAttachFile: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSelectedFiles.setLayoutManager(layoutManagerAttachFile)
        rvSelectedFilesAdapter = SelectedAttachFileAdapter(mutableListOf(), this)
        binding.rvSelectedFiles.adapter = rvSelectedFilesAdapter

        val layoutManagerQuickResponse: RecyclerView.LayoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvQuickResponse.setLayoutManager(layoutManagerQuickResponse)
        rvQuickResponseAdapter = HorizontalQuickResponseAdapter(quickMessageResponseList, this)
        binding.rvQuickResponse.adapter = rvQuickResponseAdapter
        (binding.rvQuickResponse.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    openCamera()
                } else {
                    showToast(getString(R.string.camera_permission_denied))
                }
            }

        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    selectedImageUris.add(captureImageUri)
                    rvSelectedImagesAdapter.updateData(selectedImageUris)
                    binding.rvSelectedImages.fadeIn()
                }
            }
    }

    fun updateSelectedCount(selectedCount: Int) {
        if (selectedCount != 0) {
            binding.rvSearchView.visibility = View.INVISIBLE
            binding.rvChatDetails.visibility = View.INVISIBLE

            val selectedMessagesIds =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

            if (areValidMessageIds(selectedMessagesIds)) {
                if (isContainsLink(getMessagesByIds(this, selectedMessagesIds))) {
                    if (SharedPreferencesHelper.isMessageStarred(
                            this, selectedMessagesIds[0], StarCategory.LINK, threadId.toString()
                        )
                    ) {
                        isAlreadyStar = true
                        binding.ivMessageStar.setImageDrawable(getDrawableFromAttr(R.attr.fillStar))
                    } else {
                        isAlreadyStar = false
                        binding.ivMessageStar.setImageDrawable(getDrawableFromAttr(R.attr.unfillStar))
                    }
                } else {
                    if (SharedPreferencesHelper.isMessageStarred(
                            this,
                            selectedMessagesIds[0],
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    ) {
                        isAlreadyStar = true
                        binding.ivMessageStar.setImageDrawable(getDrawableFromAttr(R.attr.fillStar))
                    } else {
                        isAlreadyStar = false
                        binding.ivMessageStar.setImageDrawable(getDrawableFromAttr(R.attr.unfillStar))
                    }
                }
                binding.ivCopyMessage.visibility = View.VISIBLE
            } else {
                if (SharedPreferencesHelper.isMessageStarred(
                        this, selectedMessagesIds[0], StarCategory.IMAGE, threadId.toString()
                    )
                ) {
                    isAlreadyStar = true
                    binding.ivMessageStar.setImageDrawable(getDrawableFromAttr(R.attr.fillStar))
                } else {
                    isAlreadyStar = false
                    binding.ivMessageStar.setImageDrawable(getDrawableFromAttr(R.attr.unfillStar))
                }

                binding.ivCopyMessage.visibility = View.GONE
            }

            if (selectedCount > 1) {
                binding.ivMessageStar.visibility = View.GONE
            } else {
                binding.ivMessageStar.visibility = View.VISIBLE
            }

            binding.rvSelectedOptions.fadeIn()

            val finalMessage = getString(R.string.selected, selectedCount.toString())
            binding.txtSelectedCount.text = finalMessage
        } else {
            binding.rvSelectedOptions.visibility = View.INVISIBLE
            binding.rvSearchView.visibility = View.INVISIBLE
            rvPersonalChatListAdapter.clearAndUpdateView()
            binding.rvChatDetails.fadeIn()
        }
    }

    private fun areValidMessageIds(messageIds: List<String>): Boolean {
        return messageIds.none { it.contains("(Images)") }
    }

    private fun onSearch(query: String) {
        matchesList = findMatchesInChatItems(finalChatList, query)
        currentMatchIndex = 0
        updateResultText()
        scrollToCurrentMatch()
    }

    private fun nextMatch() {
        if (matchesList.isNotEmpty()) {
            currentMatchIndex = (currentMatchIndex + 1) % matchesList.size
            updateResultText()
            scrollToCurrentMatch()
        }
    }

    private fun previousMatch() {
        if (matchesList.isNotEmpty()) {
            currentMatchIndex =
                if (currentMatchIndex - 1 < 0) matchesList.lastIndex else currentMatchIndex - 1
            updateResultText()
            scrollToCurrentMatch()
        }
    }

    private fun updateResultText() {
        val text = if (matchesList.isNotEmpty()) "${currentMatchIndex + 1} of ${matchesList.size}"
        else getString(R.string._0_results)
        binding.txtSearchResult.text = text
    }

    private fun scrollToCurrentMatch() {
        val match = matchesList.getOrNull(currentMatchIndex) ?: return
        binding.rvSenderChatView.scrollToPosition(match.messageIndex)
        rvPersonalChatListAdapter.updateData(finalChatList, searchQuery)
    }

    private fun findMatchesInChatItems(items: List<ChatModel>, query: String): List<MatchPosition> {
        val result = mutableListOf<MatchPosition>()
        val pattern = Regex(Regex.escape(query), RegexOption.IGNORE_CASE)

        items.forEachIndexed { index, model ->
            if (model is ChatModel.MessageItem) {
                val firstMatch = pattern.find(model.message)
                if (firstMatch != null) {
                    result.add(
                        MatchPosition(
                            index, firstMatch.range.first, firstMatch.range.last + 1
                        )
                    )
                }
            }
        }
        return result
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        captureImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, captureImageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("photo_", ".jpg", storageDir)
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.CAMERA
            ) -> {
                showToast(getString(R.string.camera_permission_is_required))
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun scrollToUnreadOrLatest(chatList: List<ChatModel>) {
        val firstUnreadIndex = chatList.indexOfFirst {
            it is ChatModel.MessageItem && !it.isRead
        }

        binding.rvSenderChatView.post {
            val layoutManager =
                binding.rvSenderChatView.layoutManager as? LinearLayoutManager ?: return@post

            if (firstUnreadIndex != -1) {
                layoutManager.scrollToPositionWithOffset(firstUnreadIndex, 100)
            } else {
                binding.rvSenderChatView.scrollToPosition(chatList.size - 1)
            }
        }
    }

    private fun getContactInfoFromPhoneNumber(
        context: Context, phoneNumber: String
    ): ContactInfo? {
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
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(senderId)
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

    private fun groupMessagesWithHeaders(messages: List<ChatModel.MessageItem>): MutableList<ChatModel> {
        if (messages.isEmpty()) return mutableListOf()

        val groupedList = mutableListOf<ChatModel>()
        var lastDateHeader: String? = null
        var lastTimeHeader: String? = null

        for (message in messages) {
            val isToday = DateUtils.isToday(message.timestamp)

            if (isToday) {
                val currentTime = formatTime(message.timestamp)
                if (currentTime != lastTimeHeader) {
                    groupedList.add(ChatModel.Header(title = message.timestamp))
                    lastTimeHeader = currentTime
                }
            } else {
                val currentDateKey = getHeaderKey(message.timestamp)
                if (currentDateKey != lastDateHeader) {
                    groupedList.add(ChatModel.Header(title = message.timestamp))
                    lastDateHeader = currentDateKey
                }
            }

            groupedList.add(message)
        }

        return groupedList
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(timestamp))
    }

    private fun getHeaderKey(timeMillis: Long): String {
        val messageDate = Date(timeMillis)
        val now = Date()

        val messageCal = Calendar.getInstance().apply { time = messageDate }
        val nowCal = Calendar.getInstance().apply { time = now }

        return when {
            DateUtils.isToday(timeMillis) -> "TODAY"
            DateUtils.isToday(timeMillis - DateUtils.DAY_IN_MILLIS) -> "YESTERDAY"
            isThisWeek(timeMillis) -> {
                SimpleDateFormat("EEE", Locale.ENGLISH).format(messageDate)
            }

            messageCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM dd", Locale.ENGLISH).format(messageDate)
            }

            else -> {
                SimpleDateFormat(
                    "MMM dd, yyyy", Locale.ENGLISH
                ).format(messageDate)
            }
        }
    }

    private fun isThisWeek(timeMillis: Long): Boolean {
        val messageCal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }
        val nowCal = Calendar.getInstance()

        val weekOfYearMsg = messageCal.get(Calendar.WEEK_OF_YEAR)
        val weekOfYearNow = nowCal.get(Calendar.WEEK_OF_YEAR)

        val yearMsg = messageCal.get(Calendar.YEAR)
        val yearNow = nowCal.get(Calendar.YEAR)

        return weekOfYearMsg == weekOfYearNow && yearMsg == yearNow
    }

    private fun getMessagesForThread(
        context: Context, threadId: Long
    ): List<ChatModel.MessageItem> {
        val allMessages = mutableListOf<ChatModel.MessageItem>()

        val smsUri = "content://sms".toUri()
        val smsProjection = arrayOf("_id", "date", "body", "type", "read")
        val smsSelection = "thread_id = ?"
        val smsArgs = arrayOf(threadId.toString())

        context.contentResolver.query(smsUri, smsProjection, smsSelection, smsArgs, "date ASC")
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("_id")
                val dateIndex = cursor.getColumnIndexOrThrow("date")
                val bodyIndex = cursor.getColumnIndexOrThrow("body")
                val typeIndex = cursor.getColumnIndexOrThrow("type")
                val readIndex = cursor.getColumnIndexOrThrow("read")

                var lastMessage: ChatModel.MessageItem? = null

                while (cursor.moveToNext()) {
                    val smsId = cursor.getLong(idIndex)
                    val timestamp = cursor.getLong(dateIndex)
                    val body = cursor.getString(bodyIndex) ?: ""
                    val type = cursor.getInt(typeIndex)
                    val isFromMe = (type != 1)
                    val isRead = cursor.getInt(readIndex) == 1

                    if (lastMessage != null && lastMessage.isFromMe == isFromMe && abs(lastMessage.timestamp - timestamp) < 100) {
                        lastMessage = lastMessage.copy(
                            message = lastMessage.message + body, timestamp = timestamp
                        )
                        allMessages[allMessages.lastIndex] = lastMessage
                    } else {
                        val newMessage = ChatModel.MessageItem(
                            smsId = smsId,
                            message = body,
                            timestamp = timestamp,
                            isFromMe = isFromMe,
                            isRead = isRead,
                            mediaUri = null
                        )
                        allMessages.add(newMessage)
                        lastMessage = newMessage
                    }
                }
            }

        val partsMap = mutableMapOf<Long, MutableList<Pair<String, Uri?>>>()
        context.contentResolver.query(
            "content://mms/part".toUri(), arrayOf("_id", "ct", "text", "mid"), null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex("_id")
            val ctIndex = cursor.getColumnIndex("ct")
            val textIndex = cursor.getColumnIndex("text")
            val midIndex = cursor.getColumnIndex("mid")

            if (idIndex == -1 || ctIndex == -1 || midIndex == -1) return@use

            while (cursor.moveToNext()) {
                val partId = cursor.getString(idIndex) ?: continue
                val contentType = cursor.getString(ctIndex) ?: continue
                val mid = cursor.getLong(midIndex)

                if (contentType.equals("application/smil", true) || contentType.equals(
                        "text/xml", true
                    )
                ) continue

                val mediaUri =
                    if (contentType.startsWith("image/") || contentType.startsWith("video/") || contentType.startsWith(
                            "audio/"
                        )
                    ) {
                        "content://mms/part/$partId".toUri()
                    } else null

                val text = if (mediaUri == null && textIndex != -1) {
                    cursor.getString(textIndex) ?: ""
                } else ""

                partsMap.getOrPut(mid) { mutableListOf() }.add(Pair(text, mediaUri))
            }
        }

        val mmsUri = "content://mms".toUri()
        val mmsProjection = arrayOf("_id", "date", "msg_box", "read")
        val mmsSelection = "thread_id = ?"
        val mmsArgs = arrayOf(threadId.toString())

        context.contentResolver.query(mmsUri, mmsProjection, mmsSelection, mmsArgs, "date ASC")
            ?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow("_id")
                val dateIndex = cursor.getColumnIndexOrThrow("date")
                val boxIndex = cursor.getColumnIndexOrThrow("msg_box")
                val readIndex = cursor.getColumnIndexOrThrow("read")

                while (cursor.moveToNext()) {
                    val mmsId = cursor.getLong(idIndex)
                    val timestamp = cursor.getLong(dateIndex) * 1000L
                    val isFromMe = cursor.getInt(boxIndex) != 1
                    val isRead = cursor.getInt(readIndex) == 1

                    val parts = partsMap[mmsId] ?: continue

                    for ((text, mediaUri) in parts) {
                        if (text.isBlank() && mediaUri == null) {
                            continue
                        }

                        allMessages.add(
                            ChatModel.MessageItem(
                                smsId = mmsId,
                                message = text,
                                timestamp = timestamp,
                                isFromMe = isFromMe,
                                isRead = isRead,
                                mediaUri = mediaUri
                            )
                        )
                    }
                }
            }

        return allMessages.sortedBy { it.timestamp }
    }

    private fun getMessagesByIds(context: Context, messageIds: List<String>): String {
        val smsUri = "content://sms".toUri()
        val projection = arrayOf("_id", "body")
        val selection = "_id IN (${messageIds.joinToString(",")})"

        val messages = mutableListOf<String>()

        context.contentResolver.query(
            smsUri, projection, selection, null, null
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndexOrThrow("body")
            while (cursor.moveToNext()) {
                messages.add(cursor.getString(bodyIndex))
            }
        }

        return messages.joinToString(separator = "\n")
    }

    private fun copiedClipMessages(copiedText: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Messages", copiedText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }

    private fun deleteMessages(context: Context, messageIds: List<String>): Boolean {
        return try {
            val smsIds = messageIds.filter { !it.contains("(Images)") }
            val mmsIds =
                messageIds.filter { it.contains("(Images)") }.map { it.replace(" (Images)", "") }

            var allDeleted = true

            smsIds.forEach { id ->
                val deleted = deleteMessage(context, id, isMms = false)
                if (!deleted) allDeleted = false
            }

            mmsIds.forEach { id ->
                val deleted = deleteMessage(context, id, isMms = true)
                if (!deleted) allDeleted = false
            }

            allDeleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun deleteMessage(context: Context, id: String, isMms: Boolean): Boolean {
        return try {
            if (isMms) {
                val partUri = "content://mms/part/$id".toUri()
                context.contentResolver.delete(partUri, null, null) > 0
            } else {
                try {
                    val uri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id.toLong())
                    contentResolver.delete(uri, null, null)
                    return true
                } catch (_: Exception) {
                    return false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun isContainsLink(message: String): Boolean {
        val urlRegex =
            """((https?://|www\.)\S+|\b[a-zA-Z0-9-]+\.(com|net|org|in|ly|me|co|io|info|xyz)(/\S*)?)""".toRegex(
                RegexOption.IGNORE_CASE
            )
        return urlRegex.containsMatchIn(message)
    }

    private fun initClickListener() {
        binding.ivSelectedMore.setOnClickListener {
            if (binding.cvMoreOptionsSpecificMessageDialog.isGone) {
                val selectedMessagesIds =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                if (selectedMessagesIds.size <= 1) {
                    binding.txtShare.visibility = View.VISIBLE
                    binding.txtViewDetails.visibility = View.VISIBLE
                } else {
                    binding.txtShare.visibility = View.GONE
                    binding.txtViewDetails.visibility = View.GONE
                }
                binding.cvMoreOptionsSpecificMessageDialog.fadeIn()
            }
        }

        binding.txtShare.setOnClickListener {
            binding.cvMoreOptionsSpecificMessageDialog.fadeOut()
            val selectedMessagesIds =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (isValidMessageId(selectedMessagesIds[0])) {
                val messageOfSMS =
                    getSmsBodyById(context = this, messageId = selectedMessagesIds[0])
                shareContentMessage(this, text = messageOfSMS)
            } else {
                val regex = Regex("^\\d+")
                val messageIdForImage = regex.find(selectedMessagesIds[0])?.value ?: ""

                val photoUri = "content://mms/part/$messageIdForImage".toUri()
                shareContentMessage(this, imageUri = photoUri)
            }
        }

        binding.txtViewDetails.setOnClickListener {
            binding.cvMoreOptionsSpecificMessageDialog.fadeOut()
            val selectedMessagesIds =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            updateSelectedCount(0)
            storeThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
            )
            rvPersonalChatListAdapter.clearAndUpdateView()

            val intent = Intent(this, SpecificMessageDetailsActivity::class.java)
            intent.putExtra("MESSAGES_ID", selectedMessagesIds[0])
            startActivity(intent)
        }

        binding.txtForward.setOnClickListener {
            binding.cvMoreOptionsSpecificMessageDialog.fadeOut()
            val intent = Intent(this, SelectRecipientsActivity::class.java)
            startActivity(intent)
        }

        binding.ivCopyMessage.setOnClickListener {
            val selectedMessagesIds =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

            val copiedText = getMessagesByIds(this, selectedMessagesIds)
            copiedClipMessages(copiedText)

            updateSelectedCount(0)
            storeThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
            )
            rvPersonalChatListAdapter.clearAndUpdateView()
        }

        binding.ivDeleteMessage.setOnClickListener {
            showDeleteSelectedMessageDialog()
        }

        binding.ivMessageStar.setOnClickListener {
            val selectedMessagesIds =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

            if (areValidMessageIds(selectedMessagesIds)) {
                if (selectedMessagesIds.size < 2) {
                    val message = getMessagesByIds(this, selectedMessagesIds)
                    if (isContainsLink(message)) {
                        SharedPreferencesHelper.toggleStar(
                            this, selectedMessagesIds[0], StarCategory.LINK, threadId.toString()
                        )
                    } else {
                        SharedPreferencesHelper.toggleStar(
                            this,
                            selectedMessagesIds[0],
                            StarCategory.TEXT_ONLY,
                            threadId.toString()
                        )
                    }
                }
            } else {
                SharedPreferencesHelper.toggleStar(
                    this, selectedMessagesIds[0], StarCategory.IMAGE, threadId.toString()
                )
            }

            if (isAlreadyStar) {
                rvPersonalChatListAdapter.updateStarredMessage(selectedMessagesIds[0])
            } else {
                binding.animationOfAdd.visibility = View.VISIBLE
                binding.animationOfAdd.playAnimation()

                Handler(Looper.getMainLooper()).postDelayed({
                    binding.animationOfAdd.visibility = View.GONE
                    rvPersonalChatListAdapter.updateStarredMessage(selectedMessagesIds[0])
                }, 1100)
            }

            updateSelectedCount(0)
            storeThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
            )
            rvPersonalChatListAdapter.clearAndUpdateView()
        }

        binding.ivRemoveSelection.setOnClickListener {
            updateSelectedCount(0)
            storeThreadIDList.clear()
            SharedPreferencesHelper.saveArrayList(
                this, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
            )
            rvPersonalChatListAdapter.clearAndUpdateView()
        }

        binding.ivCloseSearch.setOnClickListener {
            searchQuery = ""
            binding.rvSearchView.visibility = View.GONE
            rvPersonalChatListAdapter.updateData(finalChatList, searchQuery)
            scrollToUnreadOrLatest(finalChatList)
            binding.rvChatDetails.fadeIn()
        }

        binding.ivUpSide.setOnClickListener {
            previousMatch()
        }

        binding.ivDownSide.setOnClickListener {
            nextMatch()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.iClipBoardView.root.isVisible) {
                    binding.iClipBoardView.root.fadeOut()
                    isEnabled = false
                    return
                }

                val adsEnabled = SharedPreferencesHelper.getBoolean(
                    this@PersonalChatActivity, Const.IS_ADS_ENABLED, false
                )
                val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                    this@PersonalChatActivity, Const.IS_INTERSTITIAL_ENABLED, false
                )

                if (adsEnabled && interstitialEnabled) {
                    InterstitialAdHelper.showAd(this@PersonalChatActivity) {
                        isEnabled = false
                        finish()
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

        binding.ivBackSearch.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setKeyboardListener {
            binding.rvSenderChatView.postDelayed({
                binding.rvSenderChatView.scrollToPosition(
                    binding.rvSenderChatView.adapter?.itemCount?.minus(1) ?: 0
                )
            }, 100)
        }

        binding.ivEmoji.setOnClickListener {
            if (emojiPopup.isShowing) {
                emojiPopup.dismiss()
            } else {
                emojiPopup.toggle()
            }
        }

        binding.ivMore.setOnClickListener {
            if (binding.cvMoreOptionsDialog.isGone) {
                if (isSubjectField) {
                    binding.txtShowSubjectField.text = getString(R.string.hide_subject_field)
                } else {
                    binding.txtShowSubjectField.text = getString(R.string.show_subject_field)
                }
                if (binding.rvSenderNotSupport.isVisible) {
                    binding.txtShowSubjectField.visibility = View.GONE
                } else {
                    binding.txtShowSubjectField.visibility = View.VISIBLE
                }
                binding.cvMoreOptionsDialog.fadeIn()
            }
        }

        binding.txtStarred.setOnClickListener {
            binding.cvMoreOptionsDialog.fadeOut()
            val intent = Intent(this, StarredMessagesActivity::class.java)
            intent.putExtra("THREAD_ID", threadId)
            intent.putExtra("IS_SENDER_OR_NUMBER", isSenderOrNumber)
            intent.putExtra("SENDER_OR_NUMBER", senderIdOrNumber)
            intent.putExtra("THREAD_ID", threadId)
            startActivity(intent)
        }

        binding.txtConversationDetails.setOnClickListener {
            binding.cvMoreOptionsDialog.fadeOut()
            val intent = Intent(this, ChatInfoActivity::class.java)
            intent.putExtra("IS_SENDER_OR_NUMBER", isSenderOrNumber)
            intent.putExtra("SENDER_OR_NUMBER", senderIdOrNumber)
            intent.putExtra("THREAD_ID", threadId)
            startActivity(intent)
        }

        binding.txtAppearance.setOnClickListener {
            binding.cvMoreOptionsDialog.fadeOut()
            startActivity(Intent(this, AppearanceActivity::class.java))
        }

        binding.txtChatWallpaper.setOnClickListener {
            binding.cvMoreOptionsDialog.fadeOut()
            startActivity(Intent(this, ChatWallpaperActivity::class.java))
        }

        binding.txtShowSubjectField.setOnClickListener {
            binding.cvMoreOptionsDialog.fadeOut()
            if (binding.txtShowSubjectField.text.contentEquals(resources.getString(R.string.show_subject_field))) {
                isSubjectField = true
                binding.rvSubjectField.fadeIn()
            } else {
                isSubjectField = false
                binding.rvSubjectField.fadeOut()
            }
        }

        binding.ivAttach.setOnClickListener {
            if (binding.llAttachView.isVisible) {
                binding.llAttachView.fadeOut()
            } else {
                binding.rvSenderChatView.scrollToPosition(finalChatList.size - 1)
                binding.llAttachView.fadeIn()
            }
        }

        binding.rvSendMessage.setOnClickListener {
            val delayMessage = SharedPreferencesHelper.getString(
                this, Const.DELAYED_SENDING_MESSAGE_TIMING, resources.getString(R.string.no_delay)
            )

            when (delayMessage) {
                resources.getString(R.string.no_delay) -> {
                    sendMessageAfterDelay()
                }

                resources.getString(R.string._3_seconds) -> {
                    binding.ivSendMessage.visibility = View.GONE
                    binding.delayProgressbar.fadeIn()

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.delayProgressbar.visibility = View.GONE
                        binding.ivSendMessage.fadeIn()
                        sendMessageAfterDelay()
                    }, 3000)
                }

                resources.getString(R.string._5_seconds) -> {
                    binding.ivSendMessage.visibility = View.GONE
                    binding.delayProgressbar.fadeIn()

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.delayProgressbar.visibility = View.GONE
                        binding.ivSendMessage.fadeIn()
                        sendMessageAfterDelay()
                    }, 5000)
                }

                resources.getString(R.string._10_seconds) -> {
                    binding.ivSendMessage.visibility = View.GONE
                    binding.delayProgressbar.fadeIn()

                    Handler(Looper.getMainLooper()).postDelayed({
                        binding.delayProgressbar.visibility = View.GONE
                        binding.ivSendMessage.fadeIn()
                        sendMessageAfterDelay()
                    }, 10000)
                }
            }
        }

        binding.rvSenderChatView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastCompletelyVisibleItem =
                    layoutManager.findLastCompletelyVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount

                val visibleThreshold = 1

                if (lastCompletelyVisibleItem < totalItemCount - visibleThreshold) {
                    binding.ivScrollDown.fadeIn()
                } else {
                    binding.ivScrollDown.fadeOut()
                }

                // If user scrolled to the last item, mark messages as read
                if (lastCompletelyVisibleItem == totalItemCount - 1) {
                    markSmsAsRead(this@PersonalChatActivity, threadId)
                }
            }
        })

        binding.rvSelectContact.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_CONTACTS), Const.READ_CONTACTS_PERMISSION
                )
            } else {
                pickContact()
            }
        }

        binding.ivClose.setOnClickListener {
            isScheduledMessgae = false
            binding.rvScheduledView.fadeOut()
        }

        binding.rvGallary.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            checkAndRequestPermission()
        }

        binding.rvCamera.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            checkCameraPermissionAndOpen()
        }

        binding.rvScheduled.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            showScheduledTimeDialog()
        }

        binding.ivScrollDown.setOnClickListener {
            val lastPosition =
                ((binding.rvSenderChatView.layoutManager as? LinearLayoutManager)?.itemCount
                    ?: 0) - 1
            binding.rvSenderChatView.smoothScrollToPosition(lastPosition)
        }

        binding.ivAddToContacts.setOnClickListener {
            openContactEditorFromThreadId(this, threadId)
        }

        binding.ivSimCard.setOnClickListener {
            val (simList, _) = getSimDetails(this)
            if (simList.isNotEmpty()) {
                showSIMCardDetailsDialog()
            } else {
                Toast.makeText(this, getString(R.string.no_sim_card_detected), Toast.LENGTH_SHORT)
                    .show()
            }
        }

        binding.rvLocations.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            getCurrentLocation()
        }

        binding.rvClipBoard.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            binding.iClipBoardView.root.fadeIn()
        }

        binding.iClipBoardView.clipTabView.tabView.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        rvClipboardAdapter.updateData(defaultClipboardList)
                    }

                    1 -> {
                        rvClipboardAdapter.updateData(funnyClipboardList)
                    }

                    2 -> {
                        rvClipboardAdapter.updateData(loveClipboardList)
                    }

                    3 -> {
                        rvClipboardAdapter.updateData(wishesClipboardList)
                    }

                    4 -> {
                        rvClipboardAdapter.updateData(sadClipboardList)
                    }

                    5 -> {
                        rvClipboardAdapter.updateData(friendClipboardList)
                    }

                    6 -> {
                        rvClipboardAdapter.updateData(birthdayClipboardList)
                    }

                    7 -> {
                        rvClipboardAdapter.updateData(lifeClipboardList)
                    }

                    8 -> {
                        rvClipboardAdapter.updateData(motivationalClipboardList)
                    }

                    9 -> {
                        rvClipboardAdapter.updateData(savageClipboardList)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        val prefix = resources.getString(R.string.subject)
        binding.etWriteSubjectField.setText(prefix)
        binding.etWriteSubjectField.setSelection(prefix.length)

        binding.rvVoiceFiles.setOnClickListener {
            binding.llAttachView.fadeOut()
            val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak_now))
            }

            val pm = packageManager
            val activities = pm.queryIntentActivities(speechRecognizerIntent, 0)

            if (activities.isNotEmpty()) {
                startActivityForResult(speechRecognizerIntent, Const.SPEECH_REQUEST_CODE)
            } else {
                Toast.makeText(
                    this, getString(R.string.speech_not_supported), Toast.LENGTH_LONG
                ).show()
                // Optionally, open keyboard as fallback
                binding.etWriteMessage.requestFocus()
            }
        }

        binding.etWriteSubjectField.customSelectionActionModeCallback =
            object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = true
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true
                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean =
                    false

                override fun onDestroyActionMode(mode: ActionMode?) {}
            }

        binding.etWriteSubjectField.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.iClipBoardView.root.isVisible) binding.iClipBoardView.root.visibility =
                    View.GONE
            }
        }

        binding.etWriteMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.iClipBoardView.root.isVisible) binding.iClipBoardView.root.visibility =
                    View.GONE
            }
        }

        binding.etWriteSubjectField.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                if (s.isNullOrEmpty()) {
                    binding.ivSendMessage.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this@PersonalChatActivity, R.color.app_theme_color
                        )
                    )
                    binding.rvSendMessage.backgroundTintList =
                        ColorStateList.valueOf(getColorFromAttr(R.attr.itemBackgroundColor))
                } else {
                    binding.ivSendMessage.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this@PersonalChatActivity, R.color.white
                        )
                    )
                    binding.rvSendMessage.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.app_theme_color))
                }

                if (s != null && !s.startsWith(prefix)) {
                    binding.etWriteSubjectField.setText(prefix)
                    binding.etWriteSubjectField.setSelection(prefix.length)
                } else if (binding.etWriteSubjectField.selectionStart < prefix.length) {
                    binding.etWriteSubjectField.setSelection(prefix.length)
                }

                isUpdating = false
            }
        })

        binding.etWriteMessage.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) {
                    binding.ivSendMessage.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this@PersonalChatActivity, R.color.app_theme_color
                        )
                    )
                    binding.rvSendMessage.backgroundTintList =
                        ColorStateList.valueOf(getColorFromAttr(R.attr.itemBackgroundColor))
                } else {
                    binding.ivSendMessage.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this@PersonalChatActivity, R.color.white
                        )
                    )
                    binding.rvSendMessage.backgroundTintList =
                        ColorStateList.valueOf(getColor(R.color.app_theme_color))
                }
            }
        })

        binding.rvFiles.setOnClickListener {
            binding.llAttachView.visibility = View.GONE
            openFilePicker()
        }
    }

    private fun shareContentMessage(context: Context, text: String? = null, imageUri: Uri? = null) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)

            if (imageUri != null) {
                shareIntent.type = "image/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else if (!text.isNullOrEmpty()) {
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, text)
            } else {
                return
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Share via")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context, getString(R.string.no_app_found_to_share_content), Toast.LENGTH_SHORT
            ).show()
        }
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

    private fun isValidMessageId(messageId: String): Boolean {
        return !messageId.contains("(Images)")
    }

    private fun openFilePicker() {
        val mimeTypes = arrayOf("application/pdf", "image/*")
        filePickerLauncher.launch(mimeTypes)
    }

    private fun handleSelectedFiles(uris: List<Uri>) {
        for (uri in uris) {
            val fileName = getFileNameFromUri(uri)
            val mimeType = getMimeTypeFromUri(uri)
            val fileSize = formatFileSize(getFileSizeFromUri(uri))

            when {
                mimeType?.startsWith("image/") == true -> {
                    selectedImageUris.add(uri)
                }

                mimeType == "application/pdf" -> {
                    selectedFile.add(AttachFile(fileName.orEmpty(), fileSize))
                }

                mimeType?.startsWith("video/") == true -> {
                    Log.d("ABCD", "This is a video file.")
                }

                else -> {
                    Log.d("ABCD", "Unknown or unsupported file type.")
                }
            }
        }
        if (selectedImageUris.isNotEmpty()) {
            rvSelectedImagesAdapter.updateData(selectedImageUris)
            binding.rvSelectedImages.fadeIn()
        } else {
            binding.rvSelectedImages.fadeOut()
        }
        if (selectedFile.isNotEmpty()) {
            rvSelectedFilesAdapter.updateData(selectedFile)
            binding.rvSelectedFiles.fadeIn()
        } else {
            binding.rvSelectedFiles.fadeOut()
        }
    }

    private fun Context.getMimeTypeFromUri(uri: Uri): String? {
        return contentResolver.getType(uri)
    }

    private fun Context.getFileSizeFromUri(uri: Uri): Long {
        val returnCursor = contentResolver.query(uri, null, null, null, null)
        returnCursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            it.moveToFirst()
            return it.getLong(sizeIndex)
        }
        return -1L
    }

    private fun formatFileSize(sizeInBytes: Long): String {
        if (sizeInBytes <= 0) return "0 B"

        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format(locale = Locale.ENGLISH, format = "%.2f GB", gb)
            mb >= 1 -> String.format(locale = Locale.ENGLISH, format = "%.2f MB", mb)
            kb >= 1 -> String.format(locale = Locale.ENGLISH, format = "%.2f KB", kb)
            else -> "$sizeInBytes B"
        }
    }

    private fun Context.getFileNameFromUri(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Const.SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0).orEmpty()
            val finalText = binding.etWriteMessage.text.toString() + " " + spokenText
            binding.etWriteMessage.setText(finalText)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (binding.cvMoreOptionsDialog.isVisible) {
            val rect = Rect()
            binding.cvMoreOptionsDialog.getGlobalVisibleRect(rect)
            if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                binding.cvMoreOptionsDialog.fadeOut()
                return true
            }
        }

        if (binding.cvMoreOptionsSpecificMessageDialog.isVisible) {
            val rect = Rect()
            binding.cvMoreOptionsSpecificMessageDialog.getGlobalVisibleRect(rect)
            if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                binding.cvMoreOptionsSpecificMessageDialog.fadeOut()
                return true
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun getContactIdFromPhoneNumber(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

            val projection = arrayOf(ContactsContract.PhoneLookup._ID)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)
                    )
                }
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getAddressFromThreadId(context: Context, threadId: Long): String? {
        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(Telephony.Sms.ADDRESS)
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId.toString())

        val cursor = context.contentResolver.query(
            uri, projection, selection, selectionArgs, null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
            }
        }
        return null
    }

    private fun openContactEditorFromThreadId(context: Context, threadId: Long) {
        val address = getAddressFromThreadId(context, threadId)
        if (address != null) {
            val contactId = getContactIdFromPhoneNumber(context, address)
            val intent = if (contactId != null) {
                val uri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI, contactId.toLong()
                )
                Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    putExtra("finishActivityOnSaveCompleted", true)
                }
            } else {
                Intent(Intent.ACTION_INSERT).apply {
                    type = ContactsContract.RawContacts.CONTENT_TYPE
                    putExtra(ContactsContract.Intents.Insert.PHONE, address)
                    putExtra("finishActivityOnSaveCompleted", true)
                }
            }
            editContactLauncher.launch(intent)
        } else {
            val contactId = getContactIdFromPhoneNumber(context, contactNumber)
            if (contactId != null) {
                val uri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI, contactId.toLong()
                )
                val intent = Intent(Intent.ACTION_EDIT).apply {
                    setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    putExtra("finishActivityOnSaveCompleted", true)
                }
                editContactLauncher.launch(intent)
            } else {
                showToast(resources.getString(R.string.address_not_found_from_threadid))
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Const.LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        locationClient!!.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                val address = getAddressFromLatLng(this, latitude, longitude)
                val finalText = binding.etWriteMessage.text.toString() + address
                binding.etWriteMessage.setText(finalText)
            } else {
                showToast(getString(R.string.error_while_fetching_a_location))
            }
        }
    }

    private fun getAddressFromLatLng(
        context: Context, latitude: Double, longitude: Double
    ): String {
        return try {
            val geocoder = Geocoder(context, Locale.ENGLISH)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]

                val fullAddress = address.getAddressLine(0)
                return fullAddress
            } else {
                getString(R.string.no_address_found)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getString(R.string.unable_to_get_address)
        }
    }

    private fun showSIMCardDetailsDialog() {
        val dialog = Dialog(this)
        val dialogSIMCardBinding: DialogSimCardBinding =
            DialogSimCardBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogSIMCardBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        val (simList, _) = getSimDetails(this)

        when (simList.size) {
            1 -> {
                val firstCharCapital = simList[0].carrierName.first().uppercaseChar()
                dialogSIMCardBinding.txtSIMOne.text = firstCharCapital.toString()

                val finalCarrierName = simList[0].carrierName + "  (SIM 1)"
                dialogSIMCardBinding.txtSIMOneBrand.text = finalCarrierName

                if (simList[0].number.isNotEmpty()) {
                    val finalNumber = getString(R.string.phone_number, simList[0].number)
                    dialogSIMCardBinding.txtSIMOneNumber.text = finalNumber
                } else dialogSIMCardBinding.txtSIMOneNumber.visibility = View.INVISIBLE

                if (simList[0].subscriptionId == SharedPreferencesHelper.getInt(
                        this,
                        Const.SIM_SLOT_NUMBER,
                        SubscriptionManager.getDefaultSmsSubscriptionId()
                    )
                ) {
                    dialogSIMCardBinding.ivSelectSIMTwo.visibility = View.GONE
                    dialogSIMCardBinding.ivSelectSIM.visibility = View.VISIBLE
                }

                dialogSIMCardBinding.rvSIMTwoView.visibility = View.GONE
                dialogSIMCardBinding.rvSIMOneView.visibility = View.VISIBLE
            }

            2 -> {
                val firstCharCapital = simList[0].carrierName.first().uppercaseChar()
                dialogSIMCardBinding.txtSIMOne.text = firstCharCapital.toString()

                val finalCarrierName = simList[0].carrierName + getString(R.string.sim_1)
                dialogSIMCardBinding.txtSIMOneBrand.text = finalCarrierName

                if (simList[0].number.isNotEmpty()) {
                    val finalNumber = getString(R.string.phone_number, simList[0].number)
                    dialogSIMCardBinding.txtSIMOneNumber.text = finalNumber
                } else dialogSIMCardBinding.txtSIMOneNumber.visibility = View.INVISIBLE

                val firstCharCapitalSIMTwo = simList[1].displayName.first().uppercaseChar()
                dialogSIMCardBinding.txtSIMTwo.text = firstCharCapitalSIMTwo.toString()

                val finalCarrierNameTwo = simList[0].carrierName + getString(R.string.sim_2)
                dialogSIMCardBinding.txtSIMTwoBrand.text = finalCarrierNameTwo

                if (simList[1].number.isNotEmpty()) {
                    val finalNumberSIMTwo = getString(R.string.phone_number, simList[1].number)
                    dialogSIMCardBinding.txtSIMTwoNumber.text = finalNumberSIMTwo
                } else dialogSIMCardBinding.txtSIMTwoNumber.visibility = View.INVISIBLE

                if (simList[0].subscriptionId == SharedPreferencesHelper.getInt(
                        this,
                        Const.SIM_SLOT_NUMBER,
                        SubscriptionManager.getDefaultSmsSubscriptionId()
                    )
                ) {
                    dialogSIMCardBinding.ivSelectSIMTwo.visibility = View.GONE
                    dialogSIMCardBinding.ivSelectSIM.visibility = View.VISIBLE
                }

                if (simList[1].subscriptionId == SharedPreferencesHelper.getInt(
                        this,
                        Const.SIM_SLOT_NUMBER,
                        SubscriptionManager.getDefaultSmsSubscriptionId()
                    )
                ) {
                    dialogSIMCardBinding.ivSelectSIM.visibility = View.GONE
                    dialogSIMCardBinding.ivSelectSIMTwo.visibility = View.VISIBLE
                }

                dialogSIMCardBinding.rvSIMOneView.visibility = View.VISIBLE
                dialogSIMCardBinding.rvSIMTwoView.visibility = View.VISIBLE
            }

            else -> {
                val textOfSIM = getString(R.string.emergency_contact)
                val firstCharCapital = textOfSIM.first().uppercaseChar()
                dialogSIMCardBinding.txtSIMOne.text = firstCharCapital.toString()
                dialogSIMCardBinding.txtSIMOneNumber.text = textOfSIM
                dialogSIMCardBinding.txtSIMOneNumber.visibility = View.INVISIBLE

                dialogSIMCardBinding.rvSIMTwoView.visibility = View.GONE
                dialogSIMCardBinding.rvSIMOneView.visibility = View.VISIBLE
            }
        }

        dialogSIMCardBinding.rvSIMOneView.setOnClickListener {
            dialog.dismiss()
            if (simList.isNotEmpty() && simList[0].subscriptionId == SharedPreferencesHelper.getInt(
                    this, Const.SIM_SLOT_NUMBER, SubscriptionManager.getDefaultSmsSubscriptionId()
                )
            ) {
                showToast(getString(R.string.sim_1_already_set_as_default))
            } else {
                if (simList.isEmpty()) {
                    showToast(getString(R.string.no_sim_card_found))
                } else {
                    SharedPreferencesHelper.saveInt(
                        this, Const.SIM_SLOT_NUMBER, simList[0].subscriptionId
                    )
                    showToast(getString(R.string.sim_1_slot_set_as_default))
                    binding.ivSimCard.setImageDrawable(
                        ContextCompat.getDrawable(
                            this, R.drawable.ic_sim_one
                        )
                    )
                }
            }
        }

        dialogSIMCardBinding.rvSIMTwoView.setOnClickListener {
            dialog.dismiss()
            if (simList.isNotEmpty() && simList[1].subscriptionId == SharedPreferencesHelper.getInt(
                    this, Const.SIM_SLOT_NUMBER, SubscriptionManager.getDefaultSmsSubscriptionId()
                )
            ) {
                showToast(getString(R.string.sim_2_already_set_as_default))
            } else {
                if (simList.isEmpty()) {
                    showToast(getString(R.string.no_sim_card_found))
                } else {
                    SharedPreferencesHelper.saveInt(
                        this, Const.SIM_SLOT_NUMBER, simList[1].subscriptionId
                    )
                    showToast(getString(R.string.sim_2_slot_set_as_default))
                    binding.ivSimCard.setImageDrawable(
                        ContextCompat.getDrawable(
                            this, R.drawable.ic_sim_2
                        )
                    )
                }
            }
        }

        dialogSIMCardBinding.ivCloseDialog.setOnClickListener {
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

    private fun getSimDetails(context: Context): Pair<List<SimInfo>, Map<String, String>> {
        val simInfoList = mutableListOf<SimInfo>()
        val defaultNumbers = mutableMapOf<String, String>()

        val subscriptionManager =
            context.getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context, Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Pair(emptyList(), emptyMap())
        }

        val defaultCallId = SubscriptionManager.getDefaultVoiceSubscriptionId()
        val defaultSmsId = SubscriptionManager.getDefaultSmsSubscriptionId()
        val defaultDataId = SubscriptionManager.getDefaultDataSubscriptionId()

        val activeSubs = subscriptionManager.activeSubscriptionInfoList
        activeSubs?.forEach { info ->
            val simInfo = SimInfo(
                slotIndex = info.simSlotIndex,
                carrierName = info.carrierName?.toString() ?: resources.getString(R.string.unknown),
                displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex}",
                number = info.number ?: resources.getString(R.string.unknown),
                subscriptionId = info.subscriptionId,
                isDefaultCall = info.subscriptionId == defaultCallId,
                isDefaultSms = info.subscriptionId == defaultSmsId,
                isDefaultData = info.subscriptionId == defaultDataId
            )
            simInfoList.add(simInfo)

            if (simInfo.isDefaultCall) defaultNumbers["defaultCallNumber"] = simInfo.number
            if (simInfo.isDefaultSms) defaultNumbers["defaultSmsNumber"] = simInfo.number
            if (simInfo.isDefaultData) defaultNumbers["defaultDataNumber"] = simInfo.number
        }

        return Pair(simInfoList, defaultNumbers)
    }

    data class SimInfo(
        val slotIndex: Int,
        val carrierName: String,
        val displayName: String,
        val number: String,
        val subscriptionId: Int,
        val isDefaultCall: Boolean,
        val isDefaultSms: Boolean,
        val isDefaultData: Boolean
    )

    private fun sendMessageAfterDelay() {
        val urisToString = selectedImageUris.joinToString(",")
        val finalMessage = if (isSubjectField) {
            binding.etWriteSubjectField.text.toString() + "\n" + binding.etWriteMessage.text.toString()
        } else {
            binding.etWriteMessage.text.toString()
        }

        if (isScheduledMessgae) {
            val scheduleSMS = ScheduledSms(
                name = contactUserDetails?.name ?: "",
                number = contactUserDetails?.number ?: "",
                imageURIs = urisToString,
                message = finalMessage.trimEnd(),
                time = finalScheduledTime,
                contactUserPhotoUri = contactUserDetails?.photoUri ?: "",
                threadID = threadId.toString(),
                scheduledMillis = getMillisFromFormattedDate(finalScheduledTime)
            )
            sendScheduleSms(this@PersonalChatActivity, scheduleSMS)
        } else {
            sendSMS(senderIdOrNumber.toString(), finalMessage.trimEnd())
        }
    }

    private fun getMillisFromFormattedDate(dateStr: String): Long {
        return try {
            val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
            val date = format.parse(dateStr)
            date?.time ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    private fun showScheduledTimeDialog() {
        var calenderView: KalendarView?
        val dialog = Dialog(this)
        val dialogSelectDatePickerBinding: DialogSelectDatePickerBinding =
            DialogSelectDatePickerBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogSelectDatePickerBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        scheduledDate = Date()
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val formattedDate = formatter.format(scheduledDate)
        dialogSelectDatePickerBinding.txtSelectedDate.text = formattedDate

        CoroutineScope(Dispatchers.Main).launch {
            dialogSelectDatePickerBinding.ivProgress.visibility = View.VISIBLE

            calenderView = KalendarView(this@PersonalChatActivity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                isScrollContainer = false
            }

            calenderView.apply {
                findViewById<ImageView>(com.ak.R.id.previous_month)?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@PersonalChatActivity, R.drawable.ic_calender_left
                    )
                )
                findViewById<ImageView>(com.ak.R.id.next_month)?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@PersonalChatActivity, R.drawable.ic_calender_right
                    )
                )
                setTodayDateColor(R.color.app_theme_color)
                setSelectedDateIndicator(
                    ContextCompat.getDrawable(
                        this@PersonalChatActivity, R.drawable.bg_date_selector
                    )
                )
            }

            dialogSelectDatePickerBinding.flCalendarContainer.removeAllViews()
            dialogSelectDatePickerBinding.ivProgress.visibility = View.GONE
            dialogSelectDatePickerBinding.flCalendarContainer.addView(calenderView)

            calenderView.setDateSelector { selectedDate ->
                scheduledDate = selectedDate
                dialogSelectDatePickerBinding.txtSelectedDate.text =
                    convertDate(selectedDate.toString())
            }
        }

        dialogSelectDatePickerBinding.txtCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogSelectDatePickerBinding.txtOk.setOnClickListener {
            dialog.dismiss()
            showTimePickerDialog()
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

    private fun showTimePickerDialog() {
        // Get current time
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Build the time picker with current time
        val builder =
            MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H) // or CLOCK_24H
                .setHour(currentHour).setMinute(currentMinute)
                .setTheme(R.style.CustomTimePickerTheme)

        val picker = builder.build()
        picker.show(supportFragmentManager, picker.toString())

        picker.addOnPositiveButtonClickListener {
            val hour = picker.hour
            val minute = picker.minute

            // Combine scheduled date and picked time
            val calendarIn = Calendar.getInstance().apply {
                time = scheduledDate
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val selectedDateTime = calendarIn.timeInMillis
            val currentTime = System.currentTimeMillis()

            if (selectedDateTime < currentTime) {
                showToast(getString(R.string.please_select_a_future_time_for_schedule_message))
                return@addOnPositiveButtonClickListener
            }

            val isPM = hour >= 12
            val hourIn12 = if (hour == 0 || hour == 12) 12 else hour % 12
            val amPm = if (isPM) "PM" else "AM"
            val formattedTime =
                String.format(Locale.ENGLISH, "%02d:%02d %s", hourIn12, minute, amPm)

            val finalDate = convertDate(scheduledDate.toString()) + ", " + formattedTime
            finalScheduledTime = finalDate
            binding.txtScheduledTime.text = finalDate
            binding.rvScheduledView.fadeIn()
            isScheduledMessgae = true
        }
    }

    private fun sendScheduleSms(context: Context, sms: ScheduledSms) {
        if (sms.message.isBlank() && sms.imageURIs.isEmpty()) {
            showToast(getString(R.string.please_enter_a_message))
            return
        }

        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = ("package:" + context.packageName).toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                showToast(getString(R.string.please_allow_schedule_exact_alarms_permission))
                return
            }
        }

        val dbHelper = ScheduledSMSDatabaseHelper(context)
        val insertedSMS = dbHelper.insertSms(sms)

        binding.rvScheduledView.fadeOut()
        selectedImageUris.clear()
        rvSelectedImagesAdapter.updateData(selectedImageUris)
        binding.rvSelectedImages.fadeOut()
        binding.etWriteMessage.text.clear()

        val intent = Intent(context, SmsAlarmReceiver::class.java).apply {
            putExtra("id", insertedSMS.id)
            putExtra("number", sms.number)
            putExtra("message", sms.message)
            putExtra("selectedImageUri", sms.imageURIs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            insertedSMS.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, sms.scheduledMillis, pendingIntent
        )

        showToast(getString(R.string.sms_scheduled_successfully))
    }

    private fun convertDate(inputDate: String): String {
        val inputFormat =
            android.icu.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH)
        val outputFormat = android.icu.text.SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)

        return try {
            val date = inputFormat.parse(inputDate)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            e.printStackTrace()
            "Invalid Date"
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun View.fadeIn(duration: Long = 300) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(duration).setListener(null)
        }
    }

    private fun View.fadeOut(duration: Long = 300) {
        if (isVisible) {
            animate().alpha(0f).setDuration(duration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                    }
                })
        }
    }

    private fun markSmsAsRead(context: Context, threadId: Long) {
        try {
            val contentResolver = context.contentResolver
            val uri = "content://sms/inbox".toUri()
            val selection = "thread_id = ? AND read = 0"
            val selectionArgs = arrayOf(threadId.toString())

            val values = ContentValues().apply {
                put("read", 1)
            }
            contentResolver.update(uri, values, selection, selectionArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        if (message.isBlank() && selectedImageUris.isEmpty() && selectedFile.isEmpty()) {
            showToast(getString(R.string.please_enter_a_message))
            return
        }
        if (message.isNotEmpty() || selectedImageUris.isNotEmpty()) {
            sendSMSAndSimulateMMS(phoneNumber, message)
        } else {
            selectedImageUris.clear()
            selectedFile.clear()
            rvSelectedFilesAdapter.updateData(selectedFile)
            rvSelectedImagesAdapter.updateData(selectedImageUris)
            binding.rvSelectedImages.fadeOut()
            binding.rvSelectedFiles.fadeOut()
            binding.etWriteMessage.text.clear()
            if (isSubjectField) {
                binding.etWriteSubjectField.text.clear()
                binding.rvSubjectField.fadeOut()
                isSubjectField = false
            }
            selectedImageUris.clear()
            selectedFile.clear()
        }
    }

    private fun sendSMSAndSimulateMMS(phoneNumber: String, message: String) {
        if (message.isNotBlank()) {
            try {
                val defaultSmsSubId = SubscriptionManager.getDefaultSmsSubscriptionId()

                val subscriptionId =
                    SharedPreferencesHelper.getInt(this, Const.SIM_SLOT_NUMBER, defaultSmsSubId)
                val smsManager = SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

                insertSmsSent(this, phoneNumber, message)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast(getString(R.string.failed_to_send_sms))
                return
            }
        }

        if (selectedImageUris.isNotEmpty()) {
            insertMmsSent(this, phoneNumber, selectedImageUris)

            CoroutineScope(Dispatchers.IO).launch {
                val originalMessages: List<ChatModel.MessageItem> =
                    getMessagesForThread(this@PersonalChatActivity, threadId)
                finalChatList = groupMessagesWithHeaders(originalMessages)

                withContext(Dispatchers.Main) {
                    if (finalChatList.isNotEmpty()) {
                        binding.txtHint.visibility = View.GONE
                        rvPersonalChatListAdapter.updateData(finalChatList, searchQuery)
                        scrollToUnreadOrLatest(finalChatList)
                    }
                }
            }
        }

        selectedImageUris.clear()
        selectedFile.clear()
        rvSelectedFilesAdapter.updateData(selectedFile)
        rvSelectedImagesAdapter.updateData(selectedImageUris)
        binding.rvSelectedImages.fadeOut()
        binding.rvSelectedFiles.fadeOut()
        binding.etWriteMessage.text.clear()
        if (isSubjectField) {
            binding.etWriteSubjectField.text.clear()
            binding.rvSubjectField.fadeOut()
            isSubjectField = false
        }
        selectedImageUris.clear()
        selectedFile.clear()

        if (SharedPreferencesHelper.getBoolean(this, Const.DELIVERY_CONFIRMATIONS, false)) {
            showDeliveryConfirmationDialog()
        }
    }

    private fun showDeliveryConfirmationDialog() {
        val dialog = Dialog(this)
        val dialogConfirmationsBinding: ItemDeliveryConfirmationsBinding =
            ItemDeliveryConfirmationsBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogConfirmationsBinding.root)
        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        dialogConfirmationsBinding.deliveryAnimation.playAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            dialogConfirmationsBinding.deliveryAnimation.cancelAnimation()
            dialog.dismiss()
        }, 3000)

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

    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                this, permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                showToast(getString(R.string.please_allow_permission_from_settings))
            }

            else -> {
                permissionRequestLauncher.launch(permission)
            }
        }
    }

    private fun openGalleryForImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        selectImagesLauncher.launch(Intent.createChooser(intent, "Select Pictures"))
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        selectImagesLauncher.launch(Intent.createChooser(intent, "Select Pictures"))
    }

    private fun Activity.setKeyboardListener(onKeyboardOpened: () -> Unit) {
        val rootView = findViewById<View>(android.R.id.content)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            val isKeyboardNowOpen = keypadHeight > screenHeight * 0.15

            if (!translateDialog.isShowing) {
                if (isKeyboardNowOpen && !hasAutoScrolledOnKeyboard) {
                    hasAutoScrolledOnKeyboard = true
                    onKeyboardOpened()
                } else if (!isKeyboardNowOpen) {
                    hasAutoScrolledOnKeyboard = false
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Const.READ_CONTACTS_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickContact()
        } else {
            if (requestCode == Const.LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            }
        }
    }

    private fun getContactInfo(contactUri: Uri) {
        val cursor = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                val contactId = it.getString(idIndex)
                val contactName = it.getString(nameIndex)
                val hasPhone = it.getInt(hasPhoneIndex)

                var phoneNumber = "No number"
                if (hasPhone > 0) {
                    val phonesCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phonesCursor?.use { pc ->
                        if (pc.moveToFirst()) {
                            val phoneIndex =
                                pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            phoneNumber = pc.getString(phoneIndex)
                        }
                    }
                }
                val finalContactText = "Name : $contactName \nPhone : $phoneNumber"
                binding.etWriteMessage.setText(finalContactText)
            }
        }
    }

    override fun onRemoveImage(imageUri: Uri) {
        selectedImageUris.removeAll { it == imageUri }
    }

    override fun onItemImagePreviewClick(uri: Uri) {
        val intent = Intent(this, ImagePreviewActivity::class.java)
        intent.putExtra("THREAD_ID", threadId)
        intent.putExtra("CONTACT_NAME", binding.txtUserName.text.toString())
        intent.putExtra("SELECTED_IMAGE", uri.toString())
        startActivity(intent)
    }

    override fun onItemTranslateClick(item: ChatModel.MessageItem, position: Int) {
        if (isNetworkAvailable) {
            showTranslateLanguageDialog(item, position)
        } else {
            showToast(getString(R.string.please_connect_to_the_internet))
        }
    }

    private fun showTranslateLanguageDialog(item: ChatModel.MessageItem, position: Int) {
        positionSelected = position
        translateDialog = Dialog(this)
        val dialogLanguageTranslateBinding: DialogChooseLanguageTransalateBinding =
            DialogChooseLanguageTransalateBinding.inflate(LayoutInflater.from(this))
        translateDialog.setContentView(dialogLanguageTranslateBinding.root)
        translateDialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)

            val metrics = resources.displayMetrics
            window.attributes = window.attributes.apply {
                width = (metrics.widthPixels * 0.9f).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        dialogLanguageTranslateBinding.rvAllLanguages.setLayoutManager(layoutManager)
        rvLanguageAdapter = AllTranslateLanguageAdapter(mutableListOf(), this, this)
        dialogLanguageTranslateBinding.rvAllLanguages.adapter = rvLanguageAdapter

        if (allLanguagesList.isNotEmpty()) {
            rvLanguageAdapter.updateData(allLanguagesList)
        }

        dialogLanguageTranslateBinding.etSearchLanguage.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterList(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ivClose.visibility = if (!s.isNullOrEmpty()) View.VISIBLE else View.GONE
            }
        })

        dialogLanguageTranslateBinding.ivClose.setOnClickListener {
            dialogLanguageTranslateBinding.etSearchLanguage.text.clear()
        }

        dialogLanguageTranslateBinding.btnCancel.setOnClickListener {
            translateDialog.dismiss()
        }

        dialogLanguageTranslateBinding.btnApply.setOnClickListener {
            if (selectedLanguageCode.isNotEmpty()) {
                translateDialog.dismiss()
                messageTranslateViewModel.translateMessage(selectedLanguageCode, item.message)
            } else {
                showToast(getString(R.string.please_select_language))
            }

        }

        if (!isFinishing && !isDestroyed) translateDialog.show()
    }

    private fun filterList(searchQuery: String) {
        if (searchQuery.isEmpty()) {
            rvLanguageAdapter.updateData(allLanguagesList)
        } else {
            val filteredList = allLanguagesList.filter { it.language.contains(searchQuery, true) }
            rvLanguageAdapter.updateData(filteredList)
        }
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

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun listOfWallpaper() {
        if (isDarkTheme()) {
            wallpaperList.add(R.color.dark_chat_theme_1)
            wallpaperList.add(R.color.dark_chat_theme_2)
            wallpaperList.add(R.color.dark_chat_theme_3)
            wallpaperList.add(R.color.dark_chat_theme_4)
            wallpaperList.add(R.color.dark_chat_theme_5)
            wallpaperList.add(R.color.dark_chat_theme_6)
            wallpaperList.add(R.color.dark_chat_theme_7)
            wallpaperList.add(R.color.dark_chat_theme_8)
            wallpaperList.add(R.color.dark_chat_theme_9)
            wallpaperList.add(R.color.dark_chat_theme_10)
            wallpaperList.add(R.color.dark_chat_theme_11)
            wallpaperList.add(R.color.dark_chat_theme_12)
            wallpaperList.add(R.color.dark_chat_theme_13)
            wallpaperList.add(R.color.dark_chat_theme_14)
            wallpaperList.add(R.color.dark_chat_theme_15)
            wallpaperList.add(R.color.dark_chat_theme_16)
            wallpaperList.add(R.color.dark_chat_theme_17)
            wallpaperList.add(R.color.dark_chat_theme_18)
            wallpaperList.add(R.color.dark_chat_theme_19)
            wallpaperList.add(R.color.dark_chat_theme_20)
            wallpaperList.add(R.color.dark_chat_theme_21)
            wallpaperList.add(R.color.dark_chat_theme_22)
            wallpaperList.add(R.color.dark_chat_theme_23)
            wallpaperList.add(R.color.dark_chat_theme_24)
            wallpaperList.add(R.color.dark_chat_theme_25)
            wallpaperList.add(R.color.dark_chat_theme_26)
            wallpaperList.add(R.color.dark_chat_theme_27)
            wallpaperList.add(R.color.dark_chat_theme_28)
            wallpaperList.add(R.color.dark_chat_theme_29)
            wallpaperList.add(R.color.dark_chat_theme_30)
            wallpaperList.add(R.color.dark_chat_theme_31)
        } else {
            wallpaperList.add(R.color.light_chat_theme_1)
            wallpaperList.add(R.color.light_chat_theme_2)
            wallpaperList.add(R.color.light_chat_theme_3)
            wallpaperList.add(R.color.light_chat_theme_4)
            wallpaperList.add(R.color.light_chat_theme_5)
            wallpaperList.add(R.color.light_chat_theme_6)
            wallpaperList.add(R.color.light_chat_theme_7)
            wallpaperList.add(R.color.light_chat_theme_8)
            wallpaperList.add(R.color.light_chat_theme_9)
            wallpaperList.add(R.color.light_chat_theme_10)
            wallpaperList.add(R.color.light_chat_theme_11)
            wallpaperList.add(R.color.light_chat_theme_12)
            wallpaperList.add(R.color.light_chat_theme_13)
            wallpaperList.add(R.color.light_chat_theme_14)
            wallpaperList.add(R.color.light_chat_theme_15)
            wallpaperList.add(R.color.light_chat_theme_16)
            wallpaperList.add(R.color.light_chat_theme_17)
            wallpaperList.add(R.color.light_chat_theme_18)
            wallpaperList.add(R.color.light_chat_theme_19)
            wallpaperList.add(R.color.light_chat_theme_20)
            wallpaperList.add(R.color.light_chat_theme_21)
            wallpaperList.add(R.color.light_chat_theme_22)
            wallpaperList.add(R.color.light_chat_theme_23)
            wallpaperList.add(R.color.light_chat_theme_24)
            wallpaperList.add(R.color.light_chat_theme_25)
            wallpaperList.add(R.color.light_chat_theme_26)
            wallpaperList.add(R.color.light_chat_theme_27)
            wallpaperList.add(R.color.light_chat_theme_28)
            wallpaperList.add(R.color.light_chat_theme_29)
            wallpaperList.add(R.color.light_chat_theme_30)
            wallpaperList.add(R.color.light_chat_theme_31)
        }
    }

    private fun listOfChatBoxColor() {
        if (isDarkTheme()) {
            chatBoxColorList.clear()
            chatBoxColorList.add(R.color.dark_chat_default_color)
            chatBoxColorList.add(R.color.dark_chat_box_color_1)
            chatBoxColorList.add(R.color.dark_chat_box_color_2)
            chatBoxColorList.add(R.color.dark_chat_box_color_3)
            chatBoxColorList.add(R.color.dark_chat_box_color_4)
            chatBoxColorList.add(R.color.dark_chat_box_color_5)
            chatBoxColorList.add(R.color.dark_chat_box_color_6)
            chatBoxColorList.add(R.color.dark_chat_box_color_7)
            chatBoxColorList.add(R.color.dark_chat_box_color_8)
            chatBoxColorList.add(R.color.dark_chat_box_color_9)
            chatBoxColorList.add(R.color.dark_chat_box_color_10)
            chatBoxColorList.add(R.color.dark_chat_box_color_11)
            chatBoxColorList.add(R.color.dark_chat_box_color_12)
            chatBoxColorList.add(R.color.dark_chat_box_color_13)
            chatBoxColorList.add(R.color.dark_chat_box_color_14)
            chatBoxColorList.add(R.color.dark_chat_box_color_15)
            chatBoxColorList.add(R.color.dark_chat_box_color_16)
            chatBoxColorList.add(R.color.dark_chat_box_color_17)
            chatBoxColorList.add(R.color.dark_chat_box_color_18)
            chatBoxColorList.add(R.color.dark_chat_box_color_19)
            chatBoxColorList.add(R.color.dark_chat_box_color_20)
            chatBoxColorList.add(R.color.dark_chat_box_color_21)
            chatBoxColorList.add(R.color.dark_chat_box_color_22)
            chatBoxColorList.add(R.color.dark_chat_box_color_23)
            chatBoxColorList.add(R.color.dark_chat_box_color_24)
            chatBoxColorList.add(R.color.dark_chat_box_color_25)
            chatBoxColorList.add(R.color.dark_chat_box_color_26)
            chatBoxColorList.add(R.color.dark_chat_box_color_27)
            chatBoxColorList.add(R.color.dark_chat_box_color_28)
        } else {
            chatBoxColorList.clear()
            chatBoxColorList.add(R.color.light_chat_default_color)
            chatBoxColorList.add(R.color.light_chat_box_color_1)
            chatBoxColorList.add(R.color.light_chat_box_color_2)
            chatBoxColorList.add(R.color.light_chat_box_color_3)
            chatBoxColorList.add(R.color.light_chat_box_color_4)
            chatBoxColorList.add(R.color.light_chat_box_color_5)
            chatBoxColorList.add(R.color.light_chat_box_color_6)
            chatBoxColorList.add(R.color.light_chat_box_color_7)
            chatBoxColorList.add(R.color.light_chat_box_color_8)
            chatBoxColorList.add(R.color.light_chat_box_color_9)
            chatBoxColorList.add(R.color.light_chat_box_color_10)
            chatBoxColorList.add(R.color.light_chat_box_color_11)
            chatBoxColorList.add(R.color.light_chat_box_color_12)
            chatBoxColorList.add(R.color.light_chat_box_color_13)
            chatBoxColorList.add(R.color.light_chat_box_color_14)
            chatBoxColorList.add(R.color.light_chat_box_color_15)
            chatBoxColorList.add(R.color.light_chat_box_color_16)
            chatBoxColorList.add(R.color.light_chat_box_color_17)
            chatBoxColorList.add(R.color.light_chat_box_color_18)
            chatBoxColorList.add(R.color.light_chat_box_color_19)
            chatBoxColorList.add(R.color.light_chat_box_color_20)
            chatBoxColorList.add(R.color.light_chat_box_color_21)
            chatBoxColorList.add(R.color.light_chat_box_color_22)
            chatBoxColorList.add(R.color.light_chat_box_color_23)
            chatBoxColorList.add(R.color.light_chat_box_color_24)
            chatBoxColorList.add(R.color.light_chat_box_color_25)
            chatBoxColorList.add(R.color.light_chat_box_color_26)
            chatBoxColorList.add(R.color.light_chat_box_color_27)
            chatBoxColorList.add(R.color.light_chat_box_color_28)
        }
    }

    override fun onResume() {
        listOfWallpaper()
        if (isDefaultSmsApp(this)) {
            val wallpaperType =
                SharedPreferencesHelper.getString(this, Const.WALLPAPER_TYPE, "Default")
            when (wallpaperType) {
                "Default" -> {
                    Glide.with(this).load("").into(binding.chatDesign)
                    binding.rvChangeColor.setBackgroundColor(getColorFromAttr(R.attr.mainBackground))
                }

                "Gallary" -> {
                    val imageUri = SharedPreferencesHelper.getString(
                        this, Const.CHAT_WALLPAPER, Const.STRING_DEFAULT_VALUE
                    )
                    if (imageUri.isNotEmpty()) {
                        val isBlurWallpaper =
                            SharedPreferencesHelper.getBoolean(this, Const.IS_WALLPAPER_BLUR, false)
                        if (isBlurWallpaper) {
                            Glide.with(this).load(imageUri)
                                .apply(RequestOptions.bitmapTransform(BlurTransformation(10, 2)))
                                .into(binding.chatDesign)
                        } else {
                            Glide.with(this).load(imageUri).into(binding.chatDesign)
                        }
                    } else {
                        Glide.with(this).load("").into(binding.chatDesign)
                        binding.rvChangeColor.setBackgroundColor(getColorFromAttr(R.attr.mainBackground))
                    }
                }

                "Others" -> {
                    val selectedPosition =
                        SharedPreferencesHelper.getInt(this, Const.OTHERS_WALLPAPER_POSITION, -1)
                    Glide.with(this)
                        .load(ContextCompat.getDrawable(this, R.drawable.img_chat_design_view))
                        .into(binding.chatDesign)
                    if (selectedPosition in wallpaperList.indices) {
                        binding.rvChangeColor.setBackgroundColor(
                            ContextCompat.getColor(this, wallpaperList[selectedPosition])
                        )
                    }
                }
            }

            val currentChatBoxStylePosition =
                SharedPreferencesHelper.getInt(this, Const.CHAT_BOX_STYLE_POSITION, 0)
            if (currentChatBoxStylePosition != lastChatBoxStylePosition) {
                lastChatBoxStylePosition = currentChatBoxStylePosition
                rvPersonalChatListAdapter.notifyDataSetChanged()
            }

            val chatBoxColorPosition =
                SharedPreferencesHelper.getInt(this, Const.CHAT_BOX_COLOR_POSITION, -1)
            val isMeChatBoxColor =
                SharedPreferencesHelper.getBoolean(this, Const.IS_ME_CHAT_BOX_COLOR, true)
            if (latsChatBoxColorPosition != chatBoxColorPosition || lastIsMeChatBoxColor != isMeChatBoxColor) {
                latsChatBoxColorPosition = chatBoxColorPosition
                lastIsMeChatBoxColor = isMeChatBoxColor
                val chatBoxColor = chatBoxColorList[latsChatBoxColorPosition]
                rvPersonalChatListAdapter.updateChatBoxColorItem(chatBoxColor, lastIsMeChatBoxColor)
            }
            userChatDetails()
        } else {
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

    private fun isAddressBlocked(context: Context?, number: String): Boolean {
        return try {
            BlockedNumberContract.isBlocked(context, number)
        } catch (_: Exception) {
            false
        }
    }

    override fun onItemClick(language: Language) {
        selectedLanguageCode = language.code
    }

    override fun onItemClick(clipboardMessage: String) {
        binding.etWriteMessage.setText(clipboardMessage)
    }

    override fun onItemClick(file: AttachFile) {
        selectedFile.removeAll { it.fileName == file.fileName }
    }

    override fun onNetworkChange(isConnected: Boolean) {
        if (isConnected) {
            isNetworkAvailable = true
            viewModel.loadAllLanguages()
            initObserver()
        } else {
            isNetworkAvailable = false
        }
    }

    private fun registerNetworkReceiver() {
        if (!isNetworkReceiverRegistered) {
            registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            isNetworkReceiverRegistered = true
        }
    }

    private fun unregisterNetworkReceiver() {
        if (isNetworkReceiverRegistered) {
            unregisterReceiver(networkReceiver)
            isNetworkReceiverRegistered = false
        }
    }

    override fun onStop() {
        unregisterNetworkReceiver()
        super.onStop()
    }

    override fun onDestroy() {
        unregisterNetworkReceiver()
        super.onDestroy()
    }

    override fun onQuickMessageItemClick(quickResponse: QuickResponse) {
        binding.etWriteMessage.setText(quickResponse.message)
    }

    override fun onSelectedMessageClick(linkOrNumber: String, performAction: String) {
        when (performAction) {
            "CALL" -> {
                if (!linkOrNumber.isEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        linkOrNumber
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    try {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = "tel:$linkOrNumber".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(dialIntent)
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to start dialer", e)
                    }
                }
            }

            "OPEN" -> {
                val linkMessage = extractLinkFromMessage(linkOrNumber)
                openLink(this, linkMessage.toString())
            }

            "COPY_LINK" -> {
                copyTextToClipboard(this, linkOrNumber)
            }

            "WHATSAPP_MESSAGE" -> {
                if (!linkOrNumber.isEmpty() || !PhoneNumberUtils.isGlobalPhoneNumber(
                        linkOrNumber
                    )
                ) {
                    showToast(getString(R.string.this_is_not_a_phone_number))
                } else {
                    sendMessageUsingWhatsAppPackage(linkOrNumber)
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
            "((https?://|http://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}([/?#]\\S*)?)".toRegex(
                RegexOption.IGNORE_CASE
            )
        return urlPattern.find(message)?.value
    }

    private fun showDeleteSelectedMessageDialog() {
        val dialog = Dialog(this)
        val deleteMessageDialogBinding: DialogDeleteSpecificMessageBinding =
            DialogDeleteSpecificMessageBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(deleteMessageDialogBinding.root)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)

            val metrics = resources.displayMetrics
            window.attributes = window.attributes.apply {
                width = (metrics.widthPixels * 0.9f).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        deleteMessageDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        deleteMessageDialogBinding.btnYes.setOnClickListener {
            val selectedMessagesIds =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

            if (selectedMessagesIds.isEmpty()) {
                dialog.dismiss()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val allDeleted = withContext(Dispatchers.IO) {

                    val deleted = deleteMessages(this@PersonalChatActivity, selectedMessagesIds)
                    if (!deleted) return@withContext false

                    selectedMessagesIds.forEach { messageId ->
                        val category = when {
                            SharedPreferencesHelper.isMessageStarred(
                                this@PersonalChatActivity,
                                messageId,
                                StarCategory.TEXT_ONLY,
                                threadId.toString()
                            ) -> StarCategory.TEXT_ONLY

                            SharedPreferencesHelper.isMessageStarred(
                                this@PersonalChatActivity,
                                messageId,
                                StarCategory.LINK,
                                threadId.toString()
                            ) -> StarCategory.LINK

                            SharedPreferencesHelper.isMessageStarred(
                                this@PersonalChatActivity,
                                messageId,
                                StarCategory.IMAGE,
                                threadId.toString()
                            ) -> StarCategory.IMAGE

                            else -> null
                        }

                        category?.let {
                            SharedPreferencesHelper.deleteSpecificStarredMessage(
                                this@PersonalChatActivity, messageId, it, threadId.toString()
                            )
                        }
                    }

                    true
                }

                if (!allDeleted) {
                    showToast(getString(R.string.some_messages_failed_to_delete))
                    dialog.dismiss()
                    return@launch
                }

                updateSelectedCount(0)
                storeThreadIDList.clear()
                SharedPreferencesHelper.saveArrayList(
                    this@PersonalChatActivity, Const.SELECTED_MESSAGE_IDS, storeThreadIDList
                )

                delay(300)

                val finalList = withContext(Dispatchers.IO) {
                    val originalMessages = getMessagesForThread(this@PersonalChatActivity, threadId)
                    groupMessagesWithHeaders(originalMessages)
                }

                finalChatList = finalList
                binding.txtHint.visibility = if (finalList.isEmpty()) View.VISIBLE else View.GONE

                rvPersonalChatListAdapter.updateData(finalList, "")
                showToast(getString(R.string.delete_successfully))

                dialog.dismiss()
            }
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    override fun onItemClick() {

    }
}