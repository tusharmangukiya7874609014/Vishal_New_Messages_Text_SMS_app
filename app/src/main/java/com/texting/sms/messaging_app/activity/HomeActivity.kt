package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Dialog
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.ChatUserAdapter
import com.texting.sms.messaging_app.adapter.CustomMonthPickerAdapter
import com.texting.sms.messaging_app.adapter.CustomYearPickerAdapter
import com.texting.sms.messaging_app.adapter.MessageFilterAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityHomeBinding
import com.texting.sms.messaging_app.databinding.BottomSheetFilterBinding
import com.texting.sms.messaging_app.databinding.DialogBlockNumberBinding
import com.texting.sms.messaging_app.databinding.DialogDeleteConversationBinding
import com.texting.sms.messaging_app.databinding.DialogRatingStarFeedbackBinding
import com.texting.sms.messaging_app.listener.CallbackHolder
import com.texting.sms.messaging_app.listener.MonthInterface
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.listener.OnArchivedRemoveInterface
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.listener.OnClickMessagesFeature
import com.texting.sms.messaging_app.listener.OnMessageFilterInterface
import com.texting.sms.messaging_app.listener.YearInterface
import com.texting.sms.messaging_app.model.ChatUser
import com.texting.sms.messaging_app.model.MessageFilter
import com.texting.sms.messaging_app.model.MonthFile
import com.texting.sms.messaging_app.services.CallOverlayService
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import com.texting.sms.messaging_app.utils.WorkScheduler
import com.texting.sms.messaging_app.utils.getColorFromAttr
import com.texting.sms.messaging_app.utils.getDrawableFromAttr
import com.texting.sms.messaging_app.viewmodel.ContactsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

class HomeActivity : BaseActivity(), OnMessageFilterInterface, OnChatUserInterface, MonthInterface,
    YearInterface, OnClickMessagesFeature, NetworkAvailableListener {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var rvMessageFilterAdapter: MessageFilterAdapter
    private lateinit var messageFilterList: List<MessageFilter>
    private lateinit var rvMessageListAdapter: ChatUserAdapter
    private var allMessageList = emptyList<ChatUser>()
    private var allMessageWithContactsNameList = emptyList<ChatUser>()
    private lateinit var knownFilterList: List<ChatUser>
    private lateinit var unknownFilterList: List<ChatUser>
    private lateinit var otpFilterList: List<ChatUser>
    private lateinit var transactionsFilterList: List<ChatUser>
    private lateinit var offersFilterList: List<ChatUser>
    private lateinit var unReadMessagesFilterList: List<ChatUser>
    private lateinit var linkMessagesFilterList: List<ChatUser>
    private lateinit var spamMessagesFilterList: List<ChatUser>
    private lateinit var travelMessagesFilterList: List<ChatUser>
    private lateinit var shoppingMessagesFilterList: List<ChatUser>
    private lateinit var healthMessagesFilterList: List<ChatUser>
    private lateinit var billsMessagesFilterList: List<ChatUser>
    private lateinit var workMessagesFilterList: List<ChatUser>
    private var selectedMessagesFilterList: MutableList<ChatUser> = mutableListOf()
    private lateinit var rvCustomMonthAdapter: CustomMonthPickerAdapter
    private lateinit var monthNameList: List<MonthFile>
    private lateinit var rvCustomYearAdapter: CustomYearPickerAdapter
    private var currentYear = LocalDate.now().year
    private var currentMonth = getCurrentMonthShortName()
    private var isMonthFilter = false
    private var messageFilter = Const.STRING_DEFAULT_VALUE
    private var messageFilterDateWise = Const.STRING_DEFAULT_VALUE
    private var startDate = Const.STRING_DEFAULT_VALUE
    private var endDate = Const.STRING_DEFAULT_VALUE
    private var fontSize = Const.STRING_DEFAULT_VALUE
    private var itemTouchHelperLeftToRight: ItemTouchHelper? = null
    private var itemTouchHelperRightToLeft: ItemTouchHelper? = null
    private var rightToLeftSwipe = Const.STRING_DEFAULT_VALUE
    private var leftToRightSwipe = Const.STRING_DEFAULT_VALUE
    private var previousRightToLeftSwipe = Const.STRING_DEFAULT_VALUE
    private var previousLeftToRightSwipe = Const.STRING_DEFAULT_VALUE
    private lateinit var editContactLauncher: ActivityResultLauncher<Intent>
    private var storeThreadIDList = ArrayList<String>()
    private var chatSMSSelected = 0
    private var editContactThreadID = 0L
    private var backPressCount = 0
    private var isMultiSelectionEnable = false
    private var isLastProfileColorToChange = false
    private val contactsViewModel: ContactsViewModel by viewModels()
    private lateinit var smsDefaultLauncher: ActivityResultLauncher<Intent>
    private var isFirstTimeAskPermissions = true

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebaseLogEvent(
            this@HomeActivity, "HOME_PAGE", "HOME_PAGE_SHOWN"
        )
        isLastProfileColorToChange =
            SharedPreferencesHelper.getBoolean(this, Const.IS_CHANGE_PROFILE_COLOR, false)
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", -1L)
        backPressCount = SharedPreferencesHelper.getInt(this, "BACK_PRESSED", 0)
        SharedPreferencesHelper.clearBlockedList(this)
        SharedPreferencesHelper.clearDeletedThreadIds(this)

        fontSize = SharedPreferencesHelper.getString(
            this,
            Const.FONT_SIZE,
            resources.getString(R.string.normal)
        )
        previousRightToLeftSwipe = SharedPreferencesHelper.getString(
            this,
            Const.LEFT_SWIPE_ACTIONS,
            resources.getString(R.string.archive)
        )
        previousLeftToRightSwipe = SharedPreferencesHelper.getString(
            this,
            Const.RIGHT_SWIPE_ACTIONS,
            resources.getString(R.string.delete)
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTaskRoot) {
                    backPressCount++
                    SharedPreferencesHelper.saveInt(
                        this@HomeActivity, "BACK_PRESSED", backPressCount
                    )

                    if (backPressCount % 5 == 0) {
                        showRatingDialog()
                        backPressCount = 0
                        SharedPreferencesHelper.saveInt(
                            this@HomeActivity, "BACK_PRESSED", backPressCount
                        )
                    } else {
                        finish()
                    }
                } else {
                    finish()
                }
            }
        })

        if (!leftToRightSwipe.contentEquals(resources.getString(R.string.none))) {
            setupLeftToRightSwipe()
        } else {
            disableSwipeLeftToRight()
        }

        if (!rightToLeftSwipe.contentEquals(resources.getString(R.string.none))) {
            setupRightToLeftSwipe()
        } else {
            disableSwipeRightToLeft()
        }

        smsDefaultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    firebaseLogEvent(
                        this@HomeActivity, "HOME_PAGE", "SMS_DEFAULT_PERMISSION_ALLOWED"
                    )
                    afterEnablePermissionView()
                    initView()
                    initClickListener()
                    initDrawerClickListener()
                } else {
                    firebaseLogEvent(
                        this@HomeActivity, "HOME_PAGE", "SMS_DEFAULT_PERMISSION_DENIED"
                    )
                    showToast(resources.getString(R.string.permission_denied_and_app_not_set_as_default_sms_app))
                }
            }

        editContactLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    getSMSResponse()
                    val data = result.data
                    val contactUri = data?.data
                    contactUri?.let {
                        val contactName = getContactNameFromUri(this, it)
                        rvMessageListAdapter.updateAfterAddToContacts(
                            editContactThreadID,
                            contactName
                        )
                    }
                }
            }

        CallbackHolder.listener = object : OnArchivedRemoveInterface {
            override fun onUpdateTheRecyclerView(isRemoved: Boolean) {
                if (isRemoved) {
                    CoroutineScope(Dispatchers.IO).launch {
                        allMessageList = getAllSmsThreads(this@HomeActivity)

                        withContext(Dispatchers.Main) {
                            applyFilterOnMessageList(messageFilter)
                            binding.rvMessageList.scrollToPosition(0)
                        }
                    }
                }
            }
        }
        initClickListener()
        initDrawerClickListener()
        setupDrawerWidth()
    }

    /**  Drawer width setup according to screen size  **/
    private fun setupDrawerWidth() {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        val maxDrawerWidthDp = min(320f, screenWidthDp - 60f)

        val drawerWidthPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, maxDrawerWidthDp, displayMetrics
        ).toInt()

        binding.navigationView.layoutParams = binding.navigationView.layoutParams.apply {
            width = drawerWidthPx
        }
    }

    /**  Manage ads config and run ads according data  **/
    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@HomeActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@HomeActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@HomeActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@HomeActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@HomeActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("HomeActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@HomeActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@HomeActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@HomeActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val bannerAdsType = getAdsPageResponse.optString("bannerAdsType").ifEmpty {
                "adaptive"
            } ?: "adaptive"

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isAppInterstitialAdsEnabled) {
                    InterstitialAdHelper.apply {
                        loadAd(this@HomeActivity)
                    }
                }

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

    private fun showRatingDialog() {
        val dialog = Dialog(this)
        val dialogRatingStarFeedbackBinding: DialogRatingStarFeedbackBinding =
            DialogRatingStarFeedbackBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogRatingStarFeedbackBinding.root)
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

        dialogRatingStarFeedbackBinding.btnCancel.setOnClickListener {
            dialog.dismiss()

            Handler(Looper.getMainLooper()).postDelayed(
                {
                    finishAffinity()
                }, 800
            )
        }

        dialogRatingStarFeedbackBinding.btnSubmit.setOnClickListener {
            dialog.dismiss()
            openPlayStoreForReview()
        }

        dialogRatingStarFeedbackBinding.ivCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun openPlayStoreForReview() {
        val uri = "market://details?id=$packageName".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$$packageName".toUri()
                )
            )
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

    private fun setupRightToLeftSwipe() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                rvMessageListAdapter.notifyItemChanged(position)
                val chatUser = rvMessageListAdapter.getItemAt(position)

                when (rightToLeftSwipe) {
                    resources.getString(R.string.archive) -> {
                        SharedPreferencesHelper.saveArchivedThread(
                            this@HomeActivity,
                            chatUser.threadId
                        )
                        val mutableList = allMessageList.toMutableList()
                        mutableList.removeAll { it.threadId == chatUser.threadId }
                        allMessageList = mutableList.toList()
                        rvMessageListAdapter.removeAt(position)

                        val snackbar = Snackbar.make(
                            binding.rvMessageList,
                            getString(R.string.conversation_archived, 1),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.undo)) {
                                singleRestoreMessageFromArchive(chatUser.threadId.toString())
                            }

                        snackbar.setActionTextColor(
                            ContextCompat.getColor(
                                this@HomeActivity,
                                R.color.app_theme_color
                            )
                        )
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
                    }

                    resources.getString(R.string.delete) -> {
                        itemTouchHelperRightToLeft?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperRightToLeft?.attachToRecyclerView(binding.rvMessageList)

                            showDeleteConversationDialog(chatUser.threadId, position)
                        }
                    }

                    resources.getString(R.string.call) -> {
                        val phoneNumber =
                            getPhoneNumberFromThreadId(this@HomeActivity, chatUser.threadId)
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

                        itemTouchHelperRightToLeft?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperRightToLeft?.attachToRecyclerView(binding.rvMessageList)
                        }
                    }

                    resources.getString(R.string.mark_read) -> {
                        val contentValues = ContentValues().apply {
                            put("read", 1)
                        }

                        val uri = "content://sms/inbox".toUri()
                        val selection = "thread_id = ? AND read = 0"
                        val selectionArgs = arrayOf(chatUser.threadId.toString())

                        try {
                            contentResolver.update(uri, contentValues, selection, selectionArgs)
                        } catch (e: Exception) {
                            Log.e("ABCD", "Failed to mark messages as read", e)
                        }

                        itemTouchHelperRightToLeft?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperRightToLeft?.attachToRecyclerView(binding.rvMessageList)
                        }
                    }

                    resources.getString(R.string.mark_unread) -> {
                        itemTouchHelperRightToLeft?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperRightToLeft?.attachToRecyclerView(binding.rvMessageList)
                        }
                    }

                    resources.getString(R.string.add_to_private_chat) -> {
                        SharedPreferencesHelper.savePrivateThread(
                            this@HomeActivity,
                            chatUser.threadId
                        )
                        val mutableList = allMessageList.toMutableList()
                        mutableList.removeAll { it.threadId == chatUser.threadId }
                        allMessageList = mutableList.toList()
                        rvMessageListAdapter.removeAt(position)
                    }
                }

                val unreadCountMessage = allMessageList.filter { msg ->
                    msg.unreadCount != 0 &&
                            !SharedPreferencesHelper.isThreadArchived(
                                this@HomeActivity,
                                msg.threadId
                            ) &&
                            !SharedPreferencesHelper.isThreadPrivate(
                                this@HomeActivity,
                                msg.threadId
                            )
                }
                val unreadCount = unreadCountMessage.size
                rvMessageFilterAdapter.updateIconOfUnread(unreadCount)

                val unarchivedThreads = SharedPreferencesHelper.filterUnarchivedThreads(
                    this@HomeActivity,
                    allMessageList
                )

                val finalChatsList = SharedPreferencesHelper.filterNonPrivateThreads(
                    this@HomeActivity,
                    unarchivedThreads
                )

                rvMessageFilterAdapter.allMessageOfCount(finalChatsList.size)
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val maxSwipe = viewHolder.itemView.width * 0.7f
                val clampedDX = dX.coerceIn(-maxSwipe, maxSwipe)

                drawRightToLeftBackground(c, viewHolder.itemView, clampedDX)
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    clampedDX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.7f
            }
        }

        itemTouchHelperRightToLeft = ItemTouchHelper(swipeCallback)
        itemTouchHelperRightToLeft?.attachToRecyclerView(binding.rvMessageList)
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

            if (messageFilter.contains(getString(R.string.txt_selected))) {
                applyFilterOnMessageList(getString(R.string.txt_selected))
            }

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
            selectedMessagesFilterList.clear()
            if (messageFilter.contains(getString(R.string.txt_selected))) {
                afterApplyFilterUpdateMessages(selectedMessagesFilterList)
            }
            rvMessageListAdapter.clearAndUpdateView()
            binding.iSelectionHeader.root.visibility = View.GONE
            binding.rvTopHeaderView.visibility = View.VISIBLE
        }
    }

    private fun drawRightToLeftBackground(canvas: Canvas, itemView: View, dX: Float) {
        if (dX == 0f) return

        val icon = when (rightToLeftSwipe) {
            resources.getString(R.string.archive) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_archive_swipe
            ) ?: return

            resources.getString(R.string.delete) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_delete_swipe
            ) ?: return

            resources.getString(R.string.call) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_swipe_call
            ) ?: return

            resources.getString(R.string.mark_read) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_read
            ) ?: return

            resources.getString(R.string.mark_unread) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_unread_messages
            ) ?: return

            resources.getString(R.string.add_to_private_chat) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_dark_lock
            ) ?: return

            else -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_archive_swipe
            ) ?: return
        }

        val backgroundColor = ContextCompat.getColor(this, R.color.app_theme_color)
        val iconMaxSizeDp = 24
        val iconMarginEndDp = 16
        val backgroundPaddingDp = 8

        val iconMaxSizePx = dpToPx(iconMaxSizeDp)
        val iconMarginEndPx = dpToPx(iconMarginEndDp)
        val backgroundPaddingPx = dpToPx(backgroundPaddingDp)

        val paint = Paint().apply {
            color = backgroundColor
        }

        // Draw background rectangle
        canvas.drawRect(
            itemView.right.toFloat() + dX + backgroundPaddingPx,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat(),
            paint
        )

        // Calculate scaled icon dimensions (preserve aspect ratio)
        val intrinsicWidth = icon.intrinsicWidth
        val intrinsicHeight = icon.intrinsicHeight

        val scale = iconMaxSizePx / maxOf(intrinsicWidth, intrinsicHeight).toFloat()
        val iconWidth = (intrinsicWidth * scale).toInt()
        val iconHeight = (intrinsicHeight * scale).toInt()

        // Calculate icon position
        val iconTop = itemView.top + (itemView.height - iconHeight) / 2
        val iconLeft = itemView.right - iconMarginEndPx - iconWidth
        val iconRight = iconLeft + iconWidth
        val iconBottom = iconTop + iconHeight

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        icon.draw(canvas)
    }

    private fun showDeleteConversationDialog(threadId: Long, position: Int) {
        val dialog = Dialog(this)
        val deleteConversationDialogBinding: DialogDeleteConversationBinding =
            DialogDeleteConversationBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(deleteConversationDialogBinding.root)
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

        deleteConversationDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        deleteConversationDialogBinding.btnYes.setOnClickListener {
            if (deleteSmsConversation(this@HomeActivity, threadId)) {
                val mutableList = allMessageList.toMutableList()
                mutableList.removeAll { it.threadId == threadId }
                allMessageList = mutableList.toList()
                rvMessageListAdapter.removeAt(position)

                if (SharedPreferencesHelper.isPinned(this@HomeActivity, threadId)) {
                    SharedPreferencesHelper.setPinned(this, threadId, false)
                }
                if (SharedPreferencesHelper.isThreadArchived(
                        this@HomeActivity,
                        threadId
                    )
                ) {
                    SharedPreferencesHelper.removeArchivedThread(
                        this@HomeActivity,
                        threadId
                    )
                }
                if (SharedPreferencesHelper.isThreadPrivate(this@HomeActivity, threadId)) {
                    SharedPreferencesHelper.removePrivateThread(
                        this@HomeActivity,
                        threadId
                    )
                }
            }
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun showDeleteMultipleConversationDialog() {
        val dialog = Dialog(this)
        val deleteConversationDialogBinding: DialogDeleteConversationBinding =
            DialogDeleteConversationBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(deleteConversationDialogBinding.root)
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

        deleteConversationDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        deleteConversationDialogBinding.btnYes.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)

            selectedThreadID.forEach { threadId ->
                if (deleteSmsConversation(this@HomeActivity, threadId.toLong())) {
                    val mutableList = allMessageList.toMutableList()
                    mutableList.removeAll { it.threadId == threadId.toLong() }

                    allMessageList = mutableList.toList()
                    rvMessageListAdapter.removeByThreadId(threadId.toLong())

                    if (SharedPreferencesHelper.isPinned(this@HomeActivity, threadId.toLong())) {
                        SharedPreferencesHelper.setPinned(this, threadId.toLong(), false)
                    }
                    if (SharedPreferencesHelper.isThreadArchived(
                            this@HomeActivity,
                            threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.removeArchivedThread(
                            this@HomeActivity,
                            threadId.toLong()
                        )
                    }
                    if (SharedPreferencesHelper.isThreadPrivate(
                            this@HomeActivity,
                            threadId.toLong()
                        )
                    ) {
                        SharedPreferencesHelper.removePrivateThread(
                            this@HomeActivity,
                            threadId.toLong()
                        )
                    }
                }
            }

            clearSelectionViewOrUpdate()
            dialog.dismiss()
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

    private fun setupLeftToRightSwipe() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                rvMessageListAdapter.notifyItemChanged(position)
                val chatUser = rvMessageListAdapter.getItemAt(position)

                when (leftToRightSwipe) {
                    resources.getString(R.string.archive) -> {
                        SharedPreferencesHelper.saveArchivedThread(
                            this@HomeActivity,
                            chatUser.threadId
                        )
                        val mutableList = allMessageList.toMutableList()
                        mutableList.removeAll { it.threadId == chatUser.threadId }
                        allMessageList = mutableList.toList()
                        rvMessageListAdapter.removeAt(position)

                        val snackbar = Snackbar.make(
                            binding.rvMessageList,
                            getString(R.string.conversation_archived, 1),
                            Snackbar.LENGTH_LONG
                        )
                            .setAction(getString(R.string.undo)) {
                                singleRestoreMessageFromArchive(chatUser.threadId.toString())
                            }

                        snackbar.setActionTextColor(
                            ContextCompat.getColor(
                                this@HomeActivity,
                                R.color.app_theme_color
                            )
                        )
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
                    }

                    resources.getString(R.string.delete) -> {
                        itemTouchHelperLeftToRight?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperLeftToRight?.attachToRecyclerView(binding.rvMessageList)

                            showDeleteConversationDialog(chatUser.threadId, position)
                        }
                    }

                    resources.getString(R.string.call) -> {
                        val phoneNumber =
                            getPhoneNumberFromThreadId(this@HomeActivity, chatUser.threadId)
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

                        itemTouchHelperLeftToRight?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperLeftToRight?.attachToRecyclerView(binding.rvMessageList)
                        }
                    }

                    resources.getString(R.string.mark_read) -> {
                        val contentValues = ContentValues().apply {
                            put("read", 1)
                        }

                        val uri = "content://sms/inbox".toUri()
                        val selection = "thread_id = ? AND read = 0"
                        val selectionArgs = arrayOf(chatUser.threadId.toString())

                        try {
                            contentResolver.update(uri, contentValues, selection, selectionArgs)
                        } catch (e: Exception) {
                            Log.e("ABCD", "Failed to mark messages as read", e)
                        }

                        itemTouchHelperLeftToRight?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperLeftToRight?.attachToRecyclerView(binding.rvMessageList)
                        }
                    }

                    resources.getString(R.string.mark_unread) -> {
                        itemTouchHelperLeftToRight?.attachToRecyclerView(null)

                        binding.rvMessageList.post {
                            rvMessageListAdapter.notifyItemChanged(position)
                            itemTouchHelperLeftToRight?.attachToRecyclerView(binding.rvMessageList)
                        }
                    }

                    resources.getString(R.string.add_to_private_chat) -> {
                        SharedPreferencesHelper.savePrivateThread(
                            this@HomeActivity,
                            chatUser.threadId
                        )
                        val mutableList = allMessageList.toMutableList()
                        mutableList.removeAll { it.threadId == chatUser.threadId }
                        allMessageList = mutableList.toList()
                        rvMessageListAdapter.removeAt(position)
                    }
                }

                val unreadCountMessage = allMessageList.filter { msg ->
                    msg.unreadCount != 0 &&
                            !SharedPreferencesHelper.isThreadArchived(
                                this@HomeActivity,
                                msg.threadId
                            ) &&
                            !SharedPreferencesHelper.isThreadPrivate(
                                this@HomeActivity,
                                msg.threadId
                            )
                }
                val unreadCount = unreadCountMessage.size
                rvMessageFilterAdapter.updateIconOfUnread(unreadCount)

                val unarchivedThreads = SharedPreferencesHelper.filterUnarchivedThreads(
                    this@HomeActivity,
                    allMessageList
                )

                val finalChatsList = SharedPreferencesHelper.filterNonPrivateThreads(
                    this@HomeActivity,
                    unarchivedThreads
                )

                rvMessageFilterAdapter.allMessageOfCount(finalChatsList.size)
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val maxSwipe = viewHolder.itemView.width * 0.7f
                val clampedDX = dX.coerceIn(-maxSwipe, maxSwipe)

                drawLeftToRightBackground(c, viewHolder.itemView, clampedDX)
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    clampedDX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                return 0.7f
            }
        }

        itemTouchHelperLeftToRight = ItemTouchHelper(swipeCallback)
        itemTouchHelperLeftToRight?.attachToRecyclerView(binding.rvMessageList)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun drawLeftToRightBackground(canvas: Canvas, itemView: View, dX: Float) {
        if (dX == 0f) return

        val icon = when (leftToRightSwipe) {
            resources.getString(R.string.archive) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_archive_swipe
            ) ?: return

            resources.getString(R.string.delete) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_delete_swipe
            ) ?: return

            resources.getString(R.string.call) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_swipe_call
            ) ?: return

            resources.getString(R.string.mark_read) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_read
            ) ?: return

            resources.getString(R.string.mark_unread) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_unread_messages
            ) ?: return

            resources.getString(R.string.add_to_private_chat) -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_dark_lock
            ) ?: return

            else -> ContextCompat.getDrawable(
                this,
                R.drawable.ic_archive_swipe
            ) ?: return
        }

        val backgroundColor = ContextCompat.getColor(this, R.color.app_theme_color)
        val iconMaxSizeDp = 24
        val iconMarginStartDp = 16
        val backgroundPaddingDp = 8

        val iconMaxSizePx = dpToPx(iconMaxSizeDp)
        val iconMarginStartPx = dpToPx(iconMarginStartDp)
        val backgroundPaddingPx = dpToPx(backgroundPaddingDp)

        val paint = Paint().apply {
            color = backgroundColor
        }

        // Draw background rectangle
        canvas.drawRect(
            itemView.left.toFloat(),
            itemView.top.toFloat(),
            itemView.left.toFloat() + dX - backgroundPaddingPx,
            itemView.bottom.toFloat(),
            paint
        )

        // Calculate scaled icon dimensions (preserve aspect ratio)
        val intrinsicWidth = icon.intrinsicWidth
        val intrinsicHeight = icon.intrinsicHeight

        val scale = iconMaxSizePx / maxOf(intrinsicWidth, intrinsicHeight).toFloat()
        val iconWidth = (intrinsicWidth * scale).toInt()
        val iconHeight = (intrinsicHeight * scale).toInt()

        // Calculate icon position
        val iconTop = itemView.top + (itemView.height - iconHeight) / 2
        val iconLeft = itemView.left + iconMarginStartPx
        val iconRight = iconLeft + iconWidth
        val iconBottom = iconTop + iconHeight

        icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        icon.draw(canvas)
    }

    private fun getCurrentMonthShortName(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM", Locale.ENGLISH)
        return dateFormat.format(calendar.time)
    }

    private fun listOfYear(): List<Int> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return (1975..currentYear).toList()
    }

    private fun listOfMonth() {
        monthNameList = listOf(
            MonthFile(getString(R.string.january), "Jan"),
            MonthFile(getString(R.string.february), "Feb"),
            MonthFile(getString(R.string.march), "Mar"),
            MonthFile(getString(R.string.april), "Apr"),
            MonthFile(getString(R.string.may), "May"),
            MonthFile(getString(R.string.june), "Jun"),
            MonthFile(getString(R.string.july), "Jul"),
            MonthFile(getString(R.string.august), "Aug"),
            MonthFile(getString(R.string.september), "Sep"),
            MonthFile(getString(R.string.october), "Oct"),
            MonthFile(getString(R.string.november), "Nov"),
            MonthFile(getString(R.string.december), "Dec"),
        )
    }

    private fun initView() {
        /**  Preload the contact number list and store into cache list  **/
        contactsViewModel.loadContactsNumbers(this)

        WorkScheduler.initFirstSchedule(this)
        messageFilter = getString(R.string.all_messages)
        messageFilterDateWise = getString(R.string.txt_default)

        messageFilterList = listOf(
            MessageFilter(R.drawable.ic_all_messages_filter, getString(R.string.all_messages)),
            MessageFilter(R.drawable.ic_unread_filter, getString(R.string.unread)),
            MessageFilter(R.drawable.ic_otp_filter, getString(R.string.otp)),
            MessageFilter(R.drawable.ic_transactions_filter, getString(R.string.transactions)),
            MessageFilter(R.drawable.ic_known_filter, getString(R.string.known)),
            MessageFilter(R.drawable.ic_unknown_filter, getString(R.string.unknown)),
            MessageFilter(R.drawable.ic_offers_filter, getString(R.string.offers)),
            MessageFilter(R.drawable.ic_link_filter, getString(R.string.link)),
            MessageFilter(R.drawable.ic_spam_filter, getString(R.string.spam)),
            MessageFilter(R.drawable.ic_travel_filter, getString(R.string.travel)),
            MessageFilter(R.drawable.ic_shopping_filter, getString(R.string.shopping)),
            MessageFilter(R.drawable.ic_health_filter, getString(R.string.health)),
            MessageFilter(R.drawable.ic_bills_filter, getString(R.string.bills)),
            MessageFilter(R.drawable.ic_work_filter, getString(R.string.work)),
            MessageFilter(R.drawable.ic_selected_filter, getString(R.string.txt_selected))
        )

        listOfMonth()
        listOfYear()

        val layoutManagerMessage: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvMessageList.setLayoutManager(layoutManagerMessage)
        rvMessageListAdapter = ChatUserAdapter(
            mutableListOf(),
            storeThreadIDList,
            this,
            this,
            getActiveSimCount(this),
            true,
            this
        )
        binding.rvMessageList.adapter = rvMessageListAdapter

        binding.rvMessageList.setHasFixedSize(true)
        binding.rvMessageList.setItemViewCacheSize(20)
        binding.rvMessageList.isNestedScrollingEnabled = false
        (binding.rvMessageList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false

        rvMessageFilterAdapter = MessageFilterAdapter(messageFilterList, this, this)

        binding.rvMessageFilterView.apply {
            layoutManager =
                LinearLayoutManager(this@HomeActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = rvMessageFilterAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }

        receiverSMSOrMMS()
        getSMSResponse()

        binding.rvMessageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisiblePosition =
                    (binding.rvMessageList.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

                if (firstVisiblePosition > 0) {
                    binding.ivScrollUp.fadeIn()
                } else {
                    binding.ivScrollUp.fadeOut()
                }
            }
        })
    }

    private fun getSMSResponse() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                allMessageList = getAllSmsThreads(this@HomeActivity)

                withContext(Dispatchers.Main) {
                    applyFilterOnMessageList(messageFilter)
                }
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
                        if (ContextCompat.checkSelfPermission(
                                this@HomeActivity,
                                Manifest.permission.READ_SMS
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            allMessageList = getAllSmsThreads(this@HomeActivity)

                            if (messageFilter.contentEquals(resources.getString(R.string.known))) {
                                val mergedList = allMessageList.map { baseItem ->
                                    val matchedItem =
                                        allMessageWithContactsNameList.find { it.threadId == baseItem.threadId }
                                    if (matchedItem != null && !matchedItem.contactName.isNullOrEmpty()) {
                                        baseItem.copy(contactName = matchedItem.contactName)
                                    } else {
                                        baseItem
                                    }
                                }

                                allMessageList = mergedList.toMutableList()
                            }

                            withContext(Dispatchers.Main) {
                                applyFilterOnMessageList(messageFilter)
                            }
                        }
                    }
                }
            }
        )
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
            SharedPreferencesHelper.removeArchivedThread(this, threadId.toLong())
        }
        CallbackHolder.listener?.onUpdateTheRecyclerView(true)
    }

    private fun singleRestoreMessageFromArchive(messageId: String) {
        SharedPreferencesHelper.removeArchivedThread(this, messageId.toLong())
        CallbackHolder.listener?.onUpdateTheRecyclerView(true)
    }

    private fun navigateWithDrawerClose(
        delay: Long = 100, action: () -> Unit
    ) {
        lifecycleScope.launch {
            if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                binding.drawerLayout.closeDrawers()
            }

            withContext(Dispatchers.Main) {
                binding.drawerLayout.postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        action()
                    }
                }, delay)
            }
        }
        binding.drawerLayout.closeDrawers()
    }

    private fun initDrawerClickListener() {
        val privacyPolicyUrl = SharedPreferencesHelper.getString(
            this,
            Const.PRIVACY_URL, ""
        )

        binding.ivNavigationDrawer.setOnClickListener {
            if (!isDefaultSmsApp(this)) {
                if (shouldRequestSmsRole()) {
                    requestSmsRole()
                }
                return@setOnClickListener
            }

            if (!binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                binding.drawerLayout.openDrawer(binding.navigationView)
            }
        }

        binding.navigationItemView.llPrivacyPolicy.setOnClickListener {
            navigateWithDrawerClose {
                openCustomTabSafe(privacyPolicyUrl)
            }
        }

        binding.navigationItemView.llRateUs.setOnClickListener {
            navigateWithDrawerClose {
                openPlayStoreForReview()
            }
        }

        binding.navigationItemView.llShare.setOnClickListener {
            navigateWithDrawerClose {
                shareApp()
            }
        }

        binding.navigationItemView.llArchived.setOnClickListener {
            navigateWithDrawerClose {
                startActivity(Intent(this, ArchivedActivity::class.java))
            }
        }

        binding.navigationItemView.llBackupAndRestore.setOnClickListener {
            navigateWithDrawerClose {
                startActivity(Intent(this, BackupAndRestoreActivity::class.java))
            }
        }

        binding.navigationItemView.llPrivateChat.setOnClickListener {
            if (SharedPreferencesHelper.getString(
                    this@HomeActivity,
                    Const.FINAL_PASSWORD,
                    Const.STRING_DEFAULT_VALUE
                ).isNotEmpty()
            ) {
                val securityQuestions = SharedPreferencesHelper.getString(
                    this@HomeActivity,
                    Const.SECURITY_QUESTION,
                    Const.STRING_DEFAULT_VALUE
                )
                val securityAnswer = SharedPreferencesHelper.getString(
                    this@HomeActivity,
                    Const.SECURITY_QUESTION_ANSWER,
                    Const.STRING_DEFAULT_VALUE
                )
                if (securityQuestions.isNotEmpty() && securityAnswer.isNotEmpty()) {
                    val intent =
                        Intent(
                            this@HomeActivity,
                            VerifyPasswordActivity::class.java
                        )
                    startActivity(intent)
                } else {
                    val intent =
                        Intent(
                            this@HomeActivity,
                            SecurityQuestionsActivity::class.java
                        )
                    startActivity(intent)
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.drawerLayout.closeDrawers()
                }, 500)
            } else {
                val intent =
                    Intent(this@HomeActivity, SetPasswordActivity::class.java)
                startActivity(intent)
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.drawerLayout.closeDrawers()
                }, 500)
            }
        }

        binding.navigationItemView.llScheduled.setOnClickListener {
            navigateWithDrawerClose {
                startActivity(Intent(this, ScheduledMessageActivity::class.java))
            }
        }

        binding.navigationItemView.llBlocked.setOnClickListener {
            navigateWithDrawerClose {
                startActivity(Intent(this, BlockingActivity::class.java))
            }
        }

        binding.navigationItemView.llSettings.setOnClickListener {
            navigateWithDrawerClose {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
    }

    private fun openCustomTabSafe(url: String) {
        try {
            val params = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(
                    ContextCompat.getColor(this, R.color.app_theme_color)
                )
                .build()

            val intent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(params)
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .build()

            intent.launchUrl(this, url.toUri())
        } catch (_: Exception) {
            openBrowserFallback(url)
        }
    }

    private fun openBrowserFallback(url: String) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: ActivityNotFoundException) {
            // No browser installed — do nothing
        }
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
            rvMessageListAdapter.clearAndUpdateView()
            if (messageFilter.contains(getString(R.string.txt_selected))) {
                selectedMessagesFilterList.clear()
                afterApplyFilterUpdateMessages(selectedMessagesFilterList)
            }
        }

        binding.iSelectionHeader.ivArchived.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                selectedThreadID.forEach { threadId ->
                    SharedPreferencesHelper.saveArchivedThread(this, threadId.toLong())
                    rvMessageListAdapter.removeByThreadId(threadId.toLong())
                }
                clearSelectionViewOrUpdate()

                val snackbar = Snackbar.make(
                    binding.rvMessageList,
                    getString(R.string.conversation_archived, selectedThreadID.size),
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
                showToast(getString(R.string.first_select_message_at_least_one_or_more))
            }
        }

        binding.iSelectionHeader.ivPrivateChats.setOnClickListener {
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            if (selectedThreadID.isNotEmpty()) {
                selectedThreadID.forEach { threadId ->
                    SharedPreferencesHelper.savePrivateThread(this, threadId.toLong())
                    rvMessageListAdapter.removeByThreadId(threadId.toLong())
                }
                clearSelectionViewOrUpdate()
            } else {
                showToast(getString(R.string.first_select_message_at_least_one_or_more))
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
                showToast(getString(R.string.first_select_message_at_least_one_or_more))
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
                                this@HomeActivity,
                                threadId.toLong()
                            )
                        if (SharedPreferencesHelper.isBlocked(
                                this@HomeActivity,
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
                    showToast(getString(R.string.first_select_message_at_least_one_or_more))
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
                rvMessageListAdapter.updateMarkAsRead(threadId.toLong())
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

        binding.iSelectionHeader.txtDialNumber.setOnClickListener {
            binding.iSelectionHeader.cvMoreOptionsDialog.fadeOut()
            val selectedThreadID =
                SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
            selectedThreadID.forEach { threadId ->
                val phoneNumber =
                    getPhoneNumberFromThreadId(this@HomeActivity, threadId.toLong())
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
                openContactEditorFromThreadId(this@HomeActivity, threadId.toLong())
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
                val address = getAddressFromThreadId(this@HomeActivity, threadId.toLong())
                copyToClipboard(this@HomeActivity, address.toString())
            }
            clearSelectionViewOrUpdate()
        }

        binding.fabStartChat.setOnClickListener {
            checkAndRequestContactPermission()
        }

        binding.ivSearchView.setOnClickListener {
            if (!isDefaultSmsApp(this)) {
                if (shouldRequestSmsRole()) {
                    requestSmsRole()
                }
                return@setOnClickListener
            }

            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.rvFilter.setOnClickListener {
            if (!isDefaultSmsApp(this)) {
                if (shouldRequestSmsRole()) {
                    requestSmsRole()
                }
                return@setOnClickListener
            }

            showFilterBottomSheet()
        }

        binding.ivScrollUp.setOnClickListener {
            binding.rvMessageList.post {
                binding.rvMessageList.smoothScrollToPosition(0)
            }
        }

        binding.rvMultiSelection.setOnClickListener {
            if (!isDefaultSmsApp(this)) {
                if (shouldRequestSmsRole()) {
                    requestSmsRole()
                }
                return@setOnClickListener
            }

            if (isMultiSelectionEnable) {
                binding.ivMultiSelection.imageTintList = null
                isMultiSelectionEnable = false
                rvMessageListAdapter.updateSelectionView(false)
                updateSelectedCount(-1)
            } else {
                isMultiSelectionEnable = true
                binding.ivMultiSelection.imageTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        this,
                        R.color.app_theme_color
                    )
                )
                rvMessageListAdapter.updateSelectionView(true)
                updateSelectedCount(0)
            }
        }
    }

    private fun copyToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Address", code)
        clipboard.setPrimaryClip(clip)
    }

    private fun showBlockConfirmationDialog() {
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
                    getPhoneNumberOrAddressFromThreadId(this@HomeActivity, threadId.toLong())
                if (blockNumber(this@HomeActivity, contactInfo.toString())) {
                    SharedPreferencesHelper.addToBlockList(
                        this@HomeActivity,
                        contactInfo.toString()
                    )
                }
            }
            showToast(resources.getString(R.string.contact_has_been_blocked_successfully))
            rvMessageListAdapter.updateBlockContacts()
            clearSelectionViewOrUpdate()
            dialog.dismiss()
        }
        if (!isFinishing && !isDestroyed) dialog.show()
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

        dialogBlockOrUnblockBinding.rvBlockNumber.visibility = View.GONE
        dialogBlockOrUnblockBinding.txtStatement.visibility = View.VISIBLE
        val titleTxt = resources.getString(R.string.unblock)
        val subTitleTxt =
            getString(R.string.are_you_sure_you_want_to_unblock_this_conversations)
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
                    getPhoneNumberOrAddressFromThreadId(this@HomeActivity, threadId.toLong())
                unblockNumber(this@HomeActivity, contactInfo.toString())
            }
            showToast(resources.getString(R.string.contact_has_been_unblocked_successfully))
            rvMessageListAdapter.updateBlockContacts()
            clearSelectionViewOrUpdate()
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Upgrade your messaging experience ✨\n\nSend messages faster with a clean, smooth, and distraction-free app.\n\nTry it now: https://play.google.com/store/apps/details?id=$packageName"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
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

    private fun openContactEditorFromThreadId(context: Context, threadId: Long) {
        val address = getAddressFromThreadId(context, threadId)
        if (address != null) {
            val contactId = getContactIdFromPhoneNumber(context, address)
            val intent = if (contactId != null) {
                val uri = ContentUris.withAppendedId(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactId.toLong()
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
            showToast(resources.getString(R.string.address_not_found_from_threadid))
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
        applyFilterOnMessageList(messageFilter)
        updateSelectedCount(-1)
        storeThreadIDList.clear()
        SharedPreferencesHelper.saveArrayList(
            this,
            Const.SELECTED_MESSAGE_IDS,
            storeThreadIDList
        )
        rvMessageListAdapter.clearAndUpdateView()
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

    private fun applyFilterOnMessageList(filterName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            allMessageList.forEachIndexed { _, item ->
                if (item.contactName.isNullOrEmpty()) {
                    val name = getContactName(
                        this@HomeActivity,
                        item.address
                    )
                    item.contactName = name
                }
            }

            withContext(Dispatchers.Main) {
                allMessageWithContactsNameList = allMessageList.toMutableList()
            }
        }

        when (filterName) {
            getString(R.string.all_messages) -> {
                messageFilter = getString(R.string.all_messages)
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(allMessageList)
                        afterApplyFilterUpdateMessagesCount(allMessageList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(allMessageList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                        afterApplyFilterUpdateMessagesCount(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(allMessageList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                        afterApplyFilterUpdateMessagesCount(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList = filterByYear(allMessageList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                        afterApplyFilterUpdateMessagesCount(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(allMessageList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                        afterApplyFilterUpdateMessagesCount(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.unread) -> {
                messageFilter = getString(R.string.unread)
                unReadMessagesFilterList = allMessageList.filter { it.unreadCount != 0 }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(unReadMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(unReadMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(
                                unReadMessagesFilterList,
                                currentMonth,
                                currentYear
                            )
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(unReadMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(unReadMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.otp) -> {
                messageFilter = getString(R.string.otp)
                otpFilterList =
                    allMessageList.filter { isOtpMessage(it.latestMessage) && containsOtpCode(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(otpFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(otpFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(otpFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList = filterByYear(otpFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(otpFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.transactions) -> {
                messageFilter = getString(R.string.transactions)
                transactionsFilterList =
                    allMessageList.filter { isTransactional(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(transactionsFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(transactionsFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(transactionsFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(transactionsFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(transactionsFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.known) -> {
                messageFilter = getString(R.string.known)
                knownFilterList = allMessageList.filter { !it.contactName.isNullOrBlank() }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(knownFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(knownFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(knownFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList = filterByYear(knownFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(knownFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.unknown) -> {
                messageFilter = getString(R.string.unknown)
                unknownFilterList = allMessageList.filter { it.contactName.isNullOrBlank() }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(unknownFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(unknownFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(unknownFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList = filterByYear(unknownFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(unknownFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.offers) -> {
                messageFilter = getString(R.string.offers)
                offersFilterList =
                    allMessageList.filter { isPromotional(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(offersFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(offersFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(offersFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList = filterByYear(offersFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(offersFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.link) -> {
                messageFilter = getString(R.string.link)
                linkMessagesFilterList =
                    allMessageList.filter { isContainsLink(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(linkMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(linkMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(linkMessagesFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(linkMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(linkMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.spam) -> {
                messageFilter = getString(R.string.spam)
                spamMessagesFilterList =
                    allMessageList.filter { isSpam(it.latestMessage, it.address) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(spamMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(spamMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(spamMessagesFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(spamMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(spamMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.travel) -> {
                messageFilter = getString(R.string.travel)
                travelMessagesFilterList =
                    allMessageList.filter { isTravelRelated(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(travelMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(travelMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(
                                travelMessagesFilterList,
                                currentMonth,
                                currentYear
                            )
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(travelMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(travelMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.shopping) -> {
                messageFilter = getString(R.string.shopping)
                shoppingMessagesFilterList =
                    allMessageList.filter { isShoppingRelated(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(shoppingMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(shoppingMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(
                                shoppingMessagesFilterList,
                                currentMonth,
                                currentYear
                            )
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(shoppingMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(shoppingMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.health) -> {
                messageFilter = getString(R.string.health)
                healthMessagesFilterList =
                    allMessageList.filter { isHealthRelated(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(healthMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(healthMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(
                                healthMessagesFilterList,
                                currentMonth,
                                currentYear
                            )
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(healthMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(healthMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.bills) -> {
                messageFilter = getString(R.string.bills)
                billsMessagesFilterList =
                    allMessageList.filter { isBillsRelated(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(billsMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(billsMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(
                                billsMessagesFilterList,
                                currentMonth,
                                currentYear
                            )
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(billsMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(billsMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.work) -> {
                messageFilter = getString(R.string.work)
                workMessagesFilterList =
                    allMessageList.filter { isWorkRelated(it.latestMessage) }
                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(workMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(workMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(workMessagesFilterList, currentMonth, currentYear)
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(workMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(workMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }

            getString(R.string.txt_selected) -> {
                messageFilter = getString(R.string.txt_selected)
                selectedMessagesFilterList.clear()
                val selectedThreadID =
                    SharedPreferencesHelper.getArrayList(this, Const.SELECTED_MESSAGE_IDS)
                allMessageList.forEach {
                    if (selectedThreadID.contains(it.threadId.toString())) {
                        selectedMessagesFilterList.add(it)
                    }
                }

                when (messageFilterDateWise) {
                    getString(R.string.txt_default) -> {
                        afterApplyFilterUpdateMessages(selectedMessagesFilterList)
                    }

                    getString(R.string.today) -> {
                        val todayFilterList = filterTodayMessages(selectedMessagesFilterList)
                        afterApplyFilterUpdateMessages(todayFilterList)
                    }

                    getString(R.string.month) -> {
                        val monthWiseFilterList =
                            filterByMonthYear(
                                selectedMessagesFilterList,
                                currentMonth,
                                currentYear
                            )
                        afterApplyFilterUpdateMessages(monthWiseFilterList)
                    }

                    getString(R.string.year) -> {
                        val yearWiseFilterList =
                            filterByYear(selectedMessagesFilterList, currentYear)
                        afterApplyFilterUpdateMessages(yearWiseFilterList)
                    }

                    getString(R.string.date_range) -> {
                        val dateRangeFilterList =
                            filterByDateRange(selectedMessagesFilterList, startDate, endDate)
                        afterApplyFilterUpdateMessages(dateRangeFilterList)
                    }
                }
                return
            }
        }
    }

    private fun afterApplyFilterUpdateMessagesCount(filterList: List<ChatUser>) {
        val unreadCountMessage = filterList.filter { msg ->
            msg.unreadCount != 0 &&
                    !SharedPreferencesHelper.isThreadArchived(this, msg.threadId) &&
                    !SharedPreferencesHelper.isThreadPrivate(this, msg.threadId)
        }
        val unreadCount = unreadCountMessage.size

        val unarchivedThreads = SharedPreferencesHelper.filterUnarchivedThreads(
            this@HomeActivity,
            filterList
        )

        val finalChatsList = SharedPreferencesHelper.filterNonPrivateThreads(
            this@HomeActivity,
            unarchivedThreads
        )
        val deletedThreadIds = SharedPreferencesHelper.getDeletedThreadIds(this@HomeActivity)
        val filteredThreads =
            finalChatsList.filterNot { deletedThreadIds.contains(it.threadId) }
        val pinnedMessageList = filteredThreads.map {
            it.copy(
                isPinned = SharedPreferencesHelper.isPinned(
                    this@HomeActivity,
                    it.threadId
                )
            )
        }.sortedWith(
            compareByDescending<ChatUser> { it.isPinned }.thenByDescending { it.timestamp }
        )

        rvMessageFilterAdapter.allMessageOfCount(pinnedMessageList.size)
        rvMessageFilterAdapter.updateIconOfUnread(unreadCount)
    }

    override fun onFilterClick(filter: MessageFilter) {
        applyFilterOnMessageList(filter.filterName)
        binding.rvMessageList.layoutManager?.scrollToPosition(0)
    }

    private fun isOtpMessage(message: String): Boolean {
        val otpKeywords = listOf(
            "otp", "code", "password",
            "pin", "CVV", "Login code"
        )
        return otpKeywords.any { keyword ->
            message.contains(keyword, ignoreCase = true)
        }
    }

    private fun isContainsLink(message: String): Boolean {
        val urlRegex =
            """((https?://|www\.)\S+|\b[a-zA-Z0-9-]+\.(com|net|org|in|ly|me|co|io|info|xyz)(/\S*)?)"""
                .toRegex(RegexOption.IGNORE_CASE)
        return urlRegex.containsMatchIn(message)
    }

    private fun isTransactional(message: String): Boolean {
        val keywords = listOf(
            "debited",
            "credited",
            "balance",
            "transaction",
            "received",
            "payment",
            "txn",
            "upi",
            "order"
        )
        return keywords.any { message.contains(it, ignoreCase = true) }
    }

    private fun isTravelRelated(message: String): Boolean {
        val travelKeywords = listOf(
            "flight",
            "airline",
            "boarding",
            "train",
            "irctc",
            "pnr",
            "bus",
            "ticket",
            "cab",
            "taxi",
            "ola",
            "uber",
            "hotel",
            "booking",
            "reservation",
            "check-in",
            "check-out",
            "travel",
            "tour",
            "holiday",
            "trip",
            "vacation",
            "journey",
            "package",
            "deal"
        )

        return travelKeywords.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(message)
        }
    }

    private fun isShoppingRelated(message: String): Boolean {
        val shoppingKeywords = listOf(
            "shopping",
            "order",
            "ordered",
            "delivery",
            "shipped",
            "dispatched",
            "package",
            "cart",
            "wishlist",
            "buy",
            "purchase",
            "sale",
            "discount",
            "deal",
            "coupon",
            "voucher",
            "myntra",
            "ajio",
            "flipkart",
            "amazon",
            "meesho",
            "snapdeal",
            "shopclues"
        )

        return shoppingKeywords.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(message)
        }
    }

    private fun isHealthRelated(message: String): Boolean {
        val healthKeywords = listOf(
            "health",
            "doctor",
            "hospital",
            "clinic",
            "pharmacy",
            "chemist",
            "medicine",
            "medicines",
            "drug",
            "prescription",
            "appointment",
            "checkup",
            "diagnostic",
            "lab",
            "blood test",
            "x-ray",
            "scan",
            "surgery",
            "treatment",
            "vaccine",
            "vaccination",
            "covid",
            "fitness",
            "gym",
            "yoga",
            "wellness",
            "diet",
            "nutrition",
            "insurance",
            "medical",
            "ambulance",
            "emergency"
        )

        return healthKeywords.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(message)
        }
    }

    private fun isBillsRelated(message: String): Boolean {
        val billKeywords = listOf(
            "bill",
            "bills",
            "due",
            "overdue",
            "outstanding",
            "payment due",
            "amount due",
            "electricity",
            "power",
            "water",
            "gas",
            "lpg",
            "postpaid",
            "mobile bill",
            "phone bill",
            "internet",
            "broadband",
            "wifi",
            "dth",
            "cable",
            "landline",
            "utility",
            "invoice",
            "subscription",
            "renewal",
            "due date",
            "last date",
            "pay now"
        )

        return billKeywords.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(message)
        }
    }

    private fun isWorkRelated(message: String): Boolean {
        val excludeKeywords = listOf("credited", "debited", "txn", "transaction", "otp")

        val missedCallKeywords = listOf(
            "missed call",
            "last missed call",
            "call alert",
            "caller tune",
            "you have a call",
            "incoming call"
        )

        if (excludeKeywords.any { message.contains(it, ignoreCase = true) }) {
            return false
        }

        if (missedCallKeywords.any { message.contains(it, ignoreCase = true) }) {
            return false
        }

        val workKeywords = listOf(
            "work", "office", "meeting", "interview", "job",
            "career", "vacancy", "position", "hiring", "recruitment",
            "application", "resume", "cv", "candidate", "employer",
            "employee", "payslip", "attendance", "timesheet", "deadline",
            "project", "task", "team", "shift", "manager", "hr",
            "conference", "zoom", "google meet", "ms teams"
        )

        return workKeywords.any { keyword ->
            Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE)
                .containsMatchIn(message)
        }
    }

    private val trustedSenders = listOf(
        "JIO", "VI", "AIRTEL", "AXISBANK", "HDFCBANK", "ICICIBANK", "SBI",
        "AMAZON", "FLIPKART", "SWIGGY", "ZOMATO", "OLA", "UBER", "PAYTM", "GOOGLE"
    )

    private val safeMessageKeywords = listOf(
        "otp", "one time password",
        "recharge", "plan", "data balance", "validity",
        "bill", "invoice", "statement", "transaction id",
        "payment successful", "debited", "credited"
    )

    private fun isFromTrustedSender(sender: String): Boolean {
        return trustedSenders.any { sender.contains(it, ignoreCase = true) }
    }

    private fun isSafeMessage(message: String): Boolean {
        return safeMessageKeywords.any { message.contains(it, ignoreCase = true) }
    }

    private fun getSpamKeywordsFound(message: String, sender: String): List<String> {
        if (isFromTrustedSender(sender) || isSafeMessage(message)) {
            return emptyList()
        }

        val spamKeywords = listOf(
            "loan",
            "winner",
            "lottery",
            "free",
            "offer",
            "discount",
            "deal",
            "prize",
            "click here",
            "call now",
            "act fast",
            "limited time",
            "congratulations",
            "urgent"
        )

        return spamKeywords.filter { message.contains(it, ignoreCase = true) }
    }

    private fun isSpam(message: String, sender: String): Boolean {
        return getSpamKeywordsFound(message, sender).isNotEmpty()
    }

    private fun isPromotional(message: String): Boolean {
        val keywords =
            listOf(
                "offer",
                "discount",
                "sale",
                "deal",
                "win",
                "free",
                "limited time",
                "exclusive"
            )
        return keywords.any { message.contains(it, ignoreCase = true) }
    }

    private fun containsOtpCode(message: String): Boolean {
        val otpRegex = Regex("\\b\\d{4,6}\\b")
        return otpRegex.containsMatchIn(message)
    }

    private fun checkAndRequestContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                100
            )
        } else {
            startActivity(Intent(this, NewConversationActivity::class.java))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startActivity(Intent(this, NewConversationActivity::class.java))
            } else {
                showToast(getString(R.string.permission_denied))
            }
        }
    }

    override fun chatUserClick(userChatDetails: ChatUser) {
        SharedPreferencesHelper.saveLong(this, "CURRENT_THREAD_ID", userChatDetails.threadId)
        val adsEnabled = SharedPreferencesHelper.getBoolean(
            this@HomeActivity, Const.IS_ADS_ENABLED, false
        )
        val interstitialEnabled = SharedPreferencesHelper.getBoolean(
            this@HomeActivity, Const.IS_INTERSTITIAL_ENABLED, false
        )

        if (adsEnabled && interstitialEnabled) {
            InterstitialAdHelper.showAd(this@HomeActivity) {
                val intent = Intent(this@HomeActivity, PersonalChatActivity::class.java)
                intent.putExtra(Const.THREAD_ID, userChatDetails.threadId)
                intent.putExtra(Const.SENDER_ID, userChatDetails.address)
                startActivity(intent)
            }
        } else {
            val intent = Intent(this, PersonalChatActivity::class.java)
            intent.putExtra(Const.THREAD_ID, userChatDetails.threadId)
            intent.putExtra(Const.SENDER_ID, userChatDetails.address)
            startActivity(intent)
        }
    }

    private fun showFilterBottomSheet() {
        val filterBottomSheetBinding = BottomSheetFilterBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        dialog.setContentView(filterBottomSheetBinding.root)

        setTextColorFromAttrOrInt(
            filterBottomSheetBinding.txtDefault,
            filterBottomSheetBinding.txtToday,
            filterBottomSheetBinding.txtMonth,
            filterBottomSheetBinding.txtYear,
            filterBottomSheetBinding.txtDateRange
        )

        when (messageFilterDateWise) {
            getString(R.string.txt_default) -> {
                filterBottomSheetBinding.rvDefaultFilter.setBorder(2)
            }

            getString(R.string.today) -> {
                filterBottomSheetBinding.llSelectMonthOrYear.visibility = View.GONE
                filterBottomSheetBinding.llSelectSpecificYear.visibility = View.GONE
                filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

                filterBottomSheetBinding.rvTodayFilter.setBorder(2)
                setTextColorFromAttrOrInt(
                    filterBottomSheetBinding.txtToday,
                    filterBottomSheetBinding.txtDefault,
                    filterBottomSheetBinding.txtMonth,
                    filterBottomSheetBinding.txtYear,
                    filterBottomSheetBinding.txtDateRange
                )
                showOneHideOthers(
                    filterBottomSheetBinding.ivTodayFilter,
                    filterBottomSheetBinding.ivDefaultFilter,
                    filterBottomSheetBinding.ivMonthFilter,
                    filterBottomSheetBinding.ivYearFilter,
                    filterBottomSheetBinding.ivDateRangeFilter
                )
                hideStroke(
                    filterBottomSheetBinding.rvDefaultFilter,
                    filterBottomSheetBinding.rvMonthFilter,
                    filterBottomSheetBinding.rvYearFilter,
                    filterBottomSheetBinding.rvDateRangeFilter
                )
            }

            getString(R.string.month) -> {
                currentMonth = getCurrentMonthShortName()
                currentYear = LocalDate.now().year
                isMonthFilter = true
                filterBottomSheetBinding.llSelectSpecificYear.visibility = View.GONE
                filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

                filterBottomSheetBinding.rvMonthFilter.setBorder(2)
                setTextColorFromAttrOrInt(
                    filterBottomSheetBinding.txtMonth,
                    filterBottomSheetBinding.txtDefault,
                    filterBottomSheetBinding.txtToday,
                    filterBottomSheetBinding.txtYear,
                    filterBottomSheetBinding.txtDateRange
                )
                showOneHideOthers(
                    filterBottomSheetBinding.ivMonthFilter,
                    filterBottomSheetBinding.ivDefaultFilter,
                    filterBottomSheetBinding.ivTodayFilter,
                    filterBottomSheetBinding.ivYearFilter,
                    filterBottomSheetBinding.ivDateRangeFilter
                )
                hideStroke(
                    filterBottomSheetBinding.rvDefaultFilter,
                    filterBottomSheetBinding.rvTodayFilter,
                    filterBottomSheetBinding.rvYearFilter,
                    filterBottomSheetBinding.rvDateRangeFilter
                )
            }

            getString(R.string.year) -> {
                currentYear = LocalDate.now().year
                isMonthFilter = false
                filterBottomSheetBinding.llSelectMonthOrYear.visibility = View.GONE
                filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

                filterBottomSheetBinding.rvYearFilter.setBorder(2)
                setTextColorFromAttrOrInt(
                    filterBottomSheetBinding.txtYear,
                    filterBottomSheetBinding.txtDefault,
                    filterBottomSheetBinding.txtToday,
                    filterBottomSheetBinding.txtMonth,
                    filterBottomSheetBinding.txtDateRange
                )
                showOneHideOthers(
                    filterBottomSheetBinding.ivYearFilter,
                    filterBottomSheetBinding.ivDefaultFilter,
                    filterBottomSheetBinding.ivTodayFilter,
                    filterBottomSheetBinding.ivMonthFilter,
                    filterBottomSheetBinding.ivDateRangeFilter
                )
                hideStroke(
                    filterBottomSheetBinding.rvDefaultFilter,
                    filterBottomSheetBinding.rvTodayFilter,
                    filterBottomSheetBinding.rvMonthFilter,
                    filterBottomSheetBinding.rvDateRangeFilter
                )
            }

            getString(R.string.date_range) -> {
                filterBottomSheetBinding.rvDateRangeFilter.setBorder(2)

                setTextColorFromAttrOrInt(
                    filterBottomSheetBinding.txtDateRange,
                    filterBottomSheetBinding.txtDefault,
                    filterBottomSheetBinding.txtToday,
                    filterBottomSheetBinding.txtMonth,
                    filterBottomSheetBinding.txtYear
                )

                showOneHideOthers(
                    filterBottomSheetBinding.ivDateRangeFilter,
                    filterBottomSheetBinding.ivDefaultFilter,
                    filterBottomSheetBinding.ivTodayFilter,
                    filterBottomSheetBinding.ivMonthFilter,
                    filterBottomSheetBinding.ivYearFilter
                )
                hideStroke(
                    filterBottomSheetBinding.rvDefaultFilter,
                    filterBottomSheetBinding.rvTodayFilter,
                    filterBottomSheetBinding.rvMonthFilter,
                    filterBottomSheetBinding.rvYearFilter
                )
            }
        }

        filterBottomSheetBinding.rvDefaultFilter.setOnClickListener {
            messageFilterDateWise = getString(R.string.txt_default)
            filterBottomSheetBinding.llSelectMonthOrYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectSpecificYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

            filterBottomSheetBinding.rvDefaultFilter.setBorder(2)
            setTextColorFromAttrOrInt(
                filterBottomSheetBinding.txtDefault,
                filterBottomSheetBinding.txtToday,
                filterBottomSheetBinding.txtMonth,
                filterBottomSheetBinding.txtYear,
                filterBottomSheetBinding.txtDateRange
            )
            showOneHideOthers(
                filterBottomSheetBinding.ivDefaultFilter,
                filterBottomSheetBinding.ivTodayFilter,
                filterBottomSheetBinding.ivMonthFilter,
                filterBottomSheetBinding.ivYearFilter,
                filterBottomSheetBinding.ivDateRangeFilter
            )
            hideStroke(
                filterBottomSheetBinding.rvTodayFilter,
                filterBottomSheetBinding.rvMonthFilter,
                filterBottomSheetBinding.rvYearFilter,
                filterBottomSheetBinding.rvDateRangeFilter
            )
        }

        filterBottomSheetBinding.rvTodayFilter.setOnClickListener {
            messageFilterDateWise = getString(R.string.today)
            filterBottomSheetBinding.llSelectMonthOrYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectSpecificYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

            filterBottomSheetBinding.rvTodayFilter.setBorder(2)
            setTextColorFromAttrOrInt(
                filterBottomSheetBinding.txtToday,
                filterBottomSheetBinding.txtDefault,
                filterBottomSheetBinding.txtMonth,
                filterBottomSheetBinding.txtYear,
                filterBottomSheetBinding.txtDateRange
            )
            showOneHideOthers(
                filterBottomSheetBinding.ivTodayFilter,
                filterBottomSheetBinding.ivDefaultFilter,
                filterBottomSheetBinding.ivMonthFilter,
                filterBottomSheetBinding.ivYearFilter,
                filterBottomSheetBinding.ivDateRangeFilter
            )
            hideStroke(
                filterBottomSheetBinding.rvDefaultFilter,
                filterBottomSheetBinding.rvMonthFilter,
                filterBottomSheetBinding.rvYearFilter,
                filterBottomSheetBinding.rvDateRangeFilter
            )
        }

        filterBottomSheetBinding.rvMonthFilter.setOnClickListener {
            messageFilterDateWise = getString(R.string.month)
            currentMonth = getCurrentMonthShortName()
            currentYear = LocalDate.now().year
            isMonthFilter = true
            filterBottomSheetBinding.llSelectSpecificYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

            filterBottomSheetBinding.rvMonthFilter.setBorder(2)
            setTextColorFromAttrOrInt(
                filterBottomSheetBinding.txtMonth,
                filterBottomSheetBinding.txtDefault,
                filterBottomSheetBinding.txtToday,
                filterBottomSheetBinding.txtYear,
                filterBottomSheetBinding.txtDateRange
            )
            showOneHideOthers(
                filterBottomSheetBinding.ivMonthFilter,
                filterBottomSheetBinding.ivDefaultFilter,
                filterBottomSheetBinding.ivTodayFilter,
                filterBottomSheetBinding.ivYearFilter,
                filterBottomSheetBinding.ivDateRangeFilter
            )
            hideStroke(
                filterBottomSheetBinding.rvDefaultFilter,
                filterBottomSheetBinding.rvTodayFilter,
                filterBottomSheetBinding.rvYearFilter,
                filterBottomSheetBinding.rvDateRangeFilter
            )

            filterBottomSheetBinding.llSelectMonthOrYear.fadeIn()
            filterBottomSheetBinding.rvMessageFilterList.visibility = View.GONE
            filterBottomSheetBinding.rvCustomMonthSelection.fadeIn()

            val gridLayoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
            filterBottomSheetBinding.rvCustomMonthView.setLayoutManager(gridLayoutManager)
            rvCustomMonthAdapter = CustomMonthPickerAdapter(monthNameList, this, this)
            filterBottomSheetBinding.rvCustomMonthView.adapter = rvCustomMonthAdapter
        }

        filterBottomSheetBinding.txtNext.setOnClickListener {
            filterBottomSheetBinding.rvCustomMonthSelection.visibility = View.GONE
            val gridLayoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
            filterBottomSheetBinding.rvCustomYearView.setLayoutManager(gridLayoutManager)
            rvCustomYearAdapter = CustomYearPickerAdapter(listOfYear(), this, this)
            filterBottomSheetBinding.rvCustomYearView.adapter = rvCustomYearAdapter
            filterBottomSheetBinding.rvCustomYearSelection.fadeIn()
            filterBottomSheetBinding.rvCustomYearView.scrollToPosition(listOfYear().size - 1)
        }

        filterBottomSheetBinding.rvYearFilter.setOnClickListener {
            messageFilterDateWise = getString(R.string.year)
            currentYear = LocalDate.now().year
            isMonthFilter = false
            filterBottomSheetBinding.llSelectMonthOrYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectDateRange.visibility = View.GONE

            filterBottomSheetBinding.rvYearFilter.setBorder(2)
            setTextColorFromAttrOrInt(
                filterBottomSheetBinding.txtYear,
                filterBottomSheetBinding.txtDefault,
                filterBottomSheetBinding.txtToday,
                filterBottomSheetBinding.txtMonth,
                filterBottomSheetBinding.txtDateRange
            )
            showOneHideOthers(
                filterBottomSheetBinding.ivYearFilter,
                filterBottomSheetBinding.ivDefaultFilter,
                filterBottomSheetBinding.ivTodayFilter,
                filterBottomSheetBinding.ivMonthFilter,
                filterBottomSheetBinding.ivDateRangeFilter
            )
            hideStroke(
                filterBottomSheetBinding.rvDefaultFilter,
                filterBottomSheetBinding.rvTodayFilter,
                filterBottomSheetBinding.rvMonthFilter,
                filterBottomSheetBinding.rvDateRangeFilter
            )

            filterBottomSheetBinding.llSelectSpecificYear.fadeIn()

            filterBottomSheetBinding.rvMessageFilterList.visibility = View.GONE
            filterBottomSheetBinding.rvCustomMonthSelection.visibility = View.GONE
            val gridLayoutManager = GridLayoutManager(this, 3, RecyclerView.VERTICAL, false)
            filterBottomSheetBinding.rvCustomYearView.setLayoutManager(gridLayoutManager)
            rvCustomYearAdapter = CustomYearPickerAdapter(listOfYear(), this, this)
            filterBottomSheetBinding.rvCustomYearView.adapter = rvCustomYearAdapter
            filterBottomSheetBinding.rvCustomYearSelection.fadeIn()
            filterBottomSheetBinding.rvCustomYearView.scrollToPosition(listOfYear().size - 1)
        }

        filterBottomSheetBinding.rvDateRangeFilter.setOnClickListener {
            messageFilterDateWise = getString(R.string.date_range)
            filterBottomSheetBinding.llSelectMonthOrYear.visibility = View.GONE
            filterBottomSheetBinding.llSelectSpecificYear.visibility = View.GONE

            filterBottomSheetBinding.rvDateRangeFilter.setBorder(2)
            setTextColorFromAttrOrInt(
                filterBottomSheetBinding.txtDateRange,
                filterBottomSheetBinding.txtDefault,
                filterBottomSheetBinding.txtToday,
                filterBottomSheetBinding.txtMonth,
                filterBottomSheetBinding.txtYear
            )
            showOneHideOthers(
                filterBottomSheetBinding.ivDateRangeFilter,
                filterBottomSheetBinding.ivDefaultFilter,
                filterBottomSheetBinding.ivTodayFilter,
                filterBottomSheetBinding.ivMonthFilter,
                filterBottomSheetBinding.ivYearFilter
            )
            hideStroke(
                filterBottomSheetBinding.rvDefaultFilter,
                filterBottomSheetBinding.rvTodayFilter,
                filterBottomSheetBinding.rvMonthFilter,
                filterBottomSheetBinding.rvYearFilter
            )

            showDateRangePicker(this) { fromDate, toDate ->
                startDate = fromDate
                endDate = toDate
                filterBottomSheetBinding.txtSelectedFromDate.text = fromDate
                filterBottomSheetBinding.txtSelectedToDate.text = toDate
                filterBottomSheetBinding.llSelectDateRange.fadeIn()
            }
        }

        filterBottomSheetBinding.txtApply.setOnClickListener {
            val selectedYear = currentYear.toString()
            if (isMonthFilter) {
                filterBottomSheetBinding.txtSelectedMonth.text = currentMonth
                filterBottomSheetBinding.txtSelectedYear.text = selectedYear
            } else {
                filterBottomSheetBinding.txtSelectedSpecificYear.text = selectedYear
            }

            filterBottomSheetBinding.rvCustomYearSelection.visibility = View.GONE
            filterBottomSheetBinding.rvCustomMonthSelection.visibility = View.GONE
            filterBottomSheetBinding.rvMessageFilterList.fadeIn()
        }

        filterBottomSheetBinding.txtCancel.setOnClickListener {
            dialog.dismiss()
        }

        filterBottomSheetBinding.txtApplyFilter.setOnClickListener {
            when (messageFilterDateWise) {
                getString(R.string.txt_default) -> {
                    binding.ivFilter.imageTintList = null
                    when (messageFilter) {
                        getString(R.string.all_messages) -> {
                            afterApplyFilterUpdateMessages(allMessageList)
                        }

                        getString(R.string.unread) -> {
                            afterApplyFilterUpdateMessages(unReadMessagesFilterList)
                        }

                        getString(R.string.known) -> {
                            afterApplyFilterUpdateMessages(knownFilterList)
                        }

                        getString(R.string.unknown) -> {
                            afterApplyFilterUpdateMessages(unknownFilterList)
                        }

                        getString(R.string.otp) -> {
                            afterApplyFilterUpdateMessages(otpFilterList)
                        }

                        getString(R.string.transactions) -> {
                            afterApplyFilterUpdateMessages(transactionsFilterList)
                        }

                        getString(R.string.offers) -> {
                            afterApplyFilterUpdateMessages(offersFilterList)
                        }

                        getString(R.string.link) -> {
                            afterApplyFilterUpdateMessages(linkMessagesFilterList)
                        }

                        getString(R.string.spam) -> {
                            afterApplyFilterUpdateMessages(spamMessagesFilterList)
                        }

                        getString(R.string.travel) -> {
                            afterApplyFilterUpdateMessages(travelMessagesFilterList)
                        }

                        getString(R.string.shopping) -> {
                            afterApplyFilterUpdateMessages(shoppingMessagesFilterList)
                        }

                        getString(R.string.health) -> {
                            afterApplyFilterUpdateMessages(healthMessagesFilterList)
                        }

                        getString(R.string.bills) -> {
                            afterApplyFilterUpdateMessages(billsMessagesFilterList)
                        }

                        getString(R.string.work) -> {
                            afterApplyFilterUpdateMessages(workMessagesFilterList)
                        }
                    }
                    afterApplyFilterUpdateMessagesCount(allMessageList)
                }

                getString(R.string.today) -> {
                    binding.ivFilter.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this,
                            R.color.app_theme_color
                        )
                    )
                    when (messageFilter) {
                        getString(R.string.all_messages) -> {
                            val todayFilterList = filterTodayMessages(allMessageList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.unread) -> {
                            val todayFilterList = filterTodayMessages(unReadMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.known) -> {
                            val todayFilterList = filterTodayMessages(knownFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.unknown) -> {
                            val todayFilterList = filterTodayMessages(unknownFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.otp) -> {
                            val todayFilterList = filterTodayMessages(otpFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.transactions) -> {
                            val todayFilterList = filterTodayMessages(transactionsFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.offers) -> {
                            val todayFilterList = filterTodayMessages(offersFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.link) -> {
                            val todayFilterList = filterTodayMessages(linkMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.spam) -> {
                            val todayFilterList = filterTodayMessages(spamMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.travel) -> {
                            val todayFilterList = filterTodayMessages(travelMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.shopping) -> {
                            val todayFilterList =
                                filterTodayMessages(shoppingMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.health) -> {
                            val todayFilterList = filterTodayMessages(healthMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.bills) -> {
                            val todayFilterList = filterTodayMessages(billsMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }

                        getString(R.string.work) -> {
                            val todayFilterList = filterTodayMessages(workMessagesFilterList)
                            afterApplyFilterUpdateMessages(todayFilterList)
                        }
                    }

                    val todayFilterList = filterTodayMessages(allMessageList)
                    afterApplyFilterUpdateMessagesCount(todayFilterList)
                }

                getString(R.string.month) -> {
                    binding.ivFilter.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this,
                            R.color.app_theme_color
                        )
                    )
                    when (messageFilter) {
                        getString(R.string.all_messages) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(allMessageList, currentMonth, currentYear)
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.unread) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    unReadMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.known) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(knownFilterList, currentMonth, currentYear)
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.unknown) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(unknownFilterList, currentMonth, currentYear)
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.otp) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(otpFilterList, currentMonth, currentYear)
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.transactions) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    transactionsFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.offers) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(offersFilterList, currentMonth, currentYear)
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.link) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    linkMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.spam) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    spamMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.travel) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    travelMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.shopping) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    shoppingMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.health) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    healthMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.bills) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    billsMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }

                        getString(R.string.work) -> {
                            val monthWiseFilterList =
                                filterByMonthYear(
                                    workMessagesFilterList,
                                    currentMonth,
                                    currentYear
                                )
                            afterApplyFilterUpdateMessages(monthWiseFilterList)
                        }
                    }

                    val monthWiseFilterList =
                        filterByMonthYear(allMessageList, currentMonth, currentYear)
                    afterApplyFilterUpdateMessagesCount(monthWiseFilterList)
                }

                getString(R.string.year) -> {
                    binding.ivFilter.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this,
                            R.color.app_theme_color
                        )
                    )
                    when (messageFilter) {
                        getString(R.string.all_messages) -> {
                            val yearWiseFilterList = filterByYear(allMessageList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.unread) -> {
                            val yearWiseFilterList =
                                filterByYear(unReadMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.known) -> {
                            val yearWiseFilterList = filterByYear(knownFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.unknown) -> {
                            val yearWiseFilterList =
                                filterByYear(unknownFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.otp) -> {
                            val yearWiseFilterList = filterByYear(otpFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.transactions) -> {
                            val yearWiseFilterList =
                                filterByYear(transactionsFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.offers) -> {
                            val yearWiseFilterList = filterByYear(offersFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.link) -> {
                            val yearWiseFilterList =
                                filterByYear(linkMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.spam) -> {
                            val yearWiseFilterList =
                                filterByYear(spamMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.travel) -> {
                            val yearWiseFilterList =
                                filterByYear(travelMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.shopping) -> {
                            val yearWiseFilterList =
                                filterByYear(shoppingMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.health) -> {
                            val yearWiseFilterList =
                                filterByYear(healthMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.bills) -> {
                            val yearWiseFilterList =
                                filterByYear(billsMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }

                        getString(R.string.work) -> {
                            val yearWiseFilterList =
                                filterByYear(workMessagesFilterList, currentYear)
                            afterApplyFilterUpdateMessages(yearWiseFilterList)
                        }
                    }

                    val yearWiseFilterList = filterByYear(allMessageList, currentYear)
                    afterApplyFilterUpdateMessagesCount(yearWiseFilterList)
                }

                getString(R.string.date_range) -> {
                    binding.ivFilter.imageTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            this,
                            R.color.app_theme_color
                        )
                    )
                    when (messageFilter) {
                        getString(R.string.all_messages) -> {
                            val dateRangeFilterList =
                                filterByDateRange(allMessageList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.unread) -> {
                            val dateRangeFilterList =
                                filterByDateRange(unReadMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.known) -> {
                            val dateRangeFilterList =
                                filterByDateRange(knownFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.unknown) -> {
                            val dateRangeFilterList =
                                filterByDateRange(unknownFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.otp) -> {
                            val dateRangeFilterList =
                                filterByDateRange(otpFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.transactions) -> {
                            val dateRangeFilterList =
                                filterByDateRange(transactionsFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.offers) -> {
                            val dateRangeFilterList =
                                filterByDateRange(offersFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.link) -> {
                            val dateRangeFilterList =
                                filterByDateRange(linkMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.spam) -> {
                            val dateRangeFilterList =
                                filterByDateRange(spamMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.travel) -> {
                            val dateRangeFilterList =
                                filterByDateRange(travelMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.shopping) -> {
                            val dateRangeFilterList =
                                filterByDateRange(
                                    shoppingMessagesFilterList,
                                    startDate,
                                    endDate
                                )
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.health) -> {
                            val dateRangeFilterList =
                                filterByDateRange(healthMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.bills) -> {
                            val dateRangeFilterList =
                                filterByDateRange(billsMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }

                        getString(R.string.work) -> {
                            val dateRangeFilterList =
                                filterByDateRange(workMessagesFilterList, startDate, endDate)
                            afterApplyFilterUpdateMessages(dateRangeFilterList)
                        }
                    }

                    val dateRangeFilterList =
                        filterByDateRange(allMessageList, startDate, endDate)
                    afterApplyFilterUpdateMessagesCount(dateRangeFilterList)
                }
            }
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

    private fun showDateRangePicker(
        context: Context,
        onDateRangeSelected: (from: String, to: String) -> Unit
    ) {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_date_range))
            .setTheme(R.style.AppDatePickerTheme)
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { dateRange ->
            val startDate = dateRange.first
            val endDate = dateRange.second

            if (startDate != null && endDate != null) {
                val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val from = dateFormat.format(Date(startDate))
                val to = dateFormat.format(Date(endDate))
                onDateRangeSelected(from, to)
            }
        }

        dateRangePicker.show(
            (context as BaseActivity).supportFragmentManager,
            "DATE_RANGE_PICKER"
        )
    }

    private fun filterTodayMessages(chatList: List<ChatUser>): List<ChatUser> {
        val today = LocalDate.now(ZoneId.systemDefault())
        return chatList.filter {
            val messageDate = Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            messageDate.isEqual(today)
        }
    }

    private fun filterByMonthYear(
        chatList: List<ChatUser>,
        monthName: String,
        year: Int
    ): List<ChatUser> {
        val targetMonth = Month.entries.find {
            it.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .equals(monthName, ignoreCase = true)
        } ?: return emptyList()

        return chatList.filter {
            val dateTime = Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dateTime.month == targetMonth && dateTime.year == year
        }
    }

    private fun filterByYear(chatList: List<ChatUser>, year: Int): List<ChatUser> {
        return chatList.filter {
            val dateTime = Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            dateTime.year == year
        }
    }

    private fun filterByDateRange(
        chatList: List<ChatUser>,
        startDateStr: String?,
        endDateStr: String?
    ): List<ChatUser> {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

        // Validate date strings, return empty if invalid
        if (startDateStr.isNullOrBlank() || endDateStr.isNullOrBlank()) {
            return emptyList()
        }

        val startDate = try {
            LocalDate.parse(startDateStr, formatter)
        } catch (_: DateTimeParseException) {
            return emptyList()
        }

        val endDate = try {
            LocalDate.parse(endDateStr, formatter)
        } catch (_: DateTimeParseException) {
            return emptyList()
        }

        return chatList.filter {
            val dateTime = Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            (dateTime.isEqual(startDate) || dateTime.isAfter(startDate)) &&
                    (dateTime.isEqual(endDate) || dateTime.isBefore(endDate))
        }
    }

    private fun afterApplyFilterUpdateMessages(chatList: List<ChatUser>) {
        val unarchivedThreads = SharedPreferencesHelper.filterUnarchivedThreads(
            this@HomeActivity,
            chatList
        )

        val finalChatsList = SharedPreferencesHelper.filterNonPrivateThreads(
            this@HomeActivity,
            unarchivedThreads
        )

        val deletedThreadIds = SharedPreferencesHelper.getDeletedThreadIds(this@HomeActivity)

        val filteredThreads =
            finalChatsList.filterNot { deletedThreadIds.contains(it.threadId) }

        val pinnedMessageList = filteredThreads.map {
            it.copy(
                isPinned = SharedPreferencesHelper.isPinned(
                    this@HomeActivity,
                    it.threadId
                )
            )
        }.sortedWith(
            compareByDescending<ChatUser> { it.isPinned }.thenByDescending { it.timestamp }
        )

        if (pinnedMessageList.isEmpty()) {
            binding.rvMessageList.visibility = View.GONE
            binding.paginationProgress.fadeOut()
            binding.rvNoMessageView.fadeIn()
        } else {
            binding.rvNoMessageView.visibility = View.GONE
            binding.rvMessageList.fadeIn()
            rvMessageListAdapter.updateData(pinnedMessageList)
            binding.paginationProgress.fadeOut()
            binding.rvMessageList.alpha = 1f
        }
    }

    private fun showOneHideOthers(visibleView: View, vararg hiddenViews: View) {
        visibleView.visibility = View.VISIBLE
        hiddenViews.forEach { it.visibility = View.INVISIBLE }
    }

    private fun hideStroke(vararg hiddenViews: View) {
        hiddenViews.forEach { it.background = getDrawableFromAttr(R.attr.itemBackground) }
    }

    private fun setTextColorFromAttrOrInt(changeView: TextView, vararg hiddenViews: TextView) {
        hiddenViews.forEach {
            it.setTextColor(getColorFromAttr(R.attr.titleTextColor))
        }
        changeView.setTextColor(ContextCompat.getColor(this, R.color.app_theme_color))
    }

    private fun View.setBorder(
        borderWidth: Int,
        cornerRadius: Float = 20f,
        backgroundColor: Int = context.getColorFromAttr(R.attr.itemBackgroundColor)
    ) {
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            setStroke(
                borderWidth,
                ContextCompat.getColor(this@HomeActivity, R.color.app_theme_color)
            )
            this.cornerRadius = cornerRadius
        }
        background = drawable
    }

    override fun onSelectedMonthClick(monthDetails: MonthFile) {
        currentMonth = monthDetails.monthName
    }

    override fun onSelectedYearClick(year: Int) {
        currentYear = year
    }

    private fun isFullScreenNotificationAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else true
    }

    override fun onResume() {
        navigateWithDrawerClose {

        }
        val isEnablePostCallScreen =
            SharedPreferencesHelper.getBoolean(this, Const.ENABLE_AFTER_CALL_SCREEN, true)

        if (isFullScreenNotificationAllowed() || Settings.canDrawOverlays(this)) {
            if (isEnablePostCallScreen && !CallOverlayService.isRunning) {
                val serviceIntent = Intent(this, CallOverlayService::class.java).apply {
                    putExtra("CALL_STATE", "10000")
                }

                try {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } catch (e: Exception) {
                    Log.d("ABCD","Overlay Service :- ${e.localizedMessage}")
                }
            }
        }

        if (isDefaultSmsApp(this)) {
            if (::rvMessageListAdapter.isInitialized) {
                val lastThreadID =
                    SharedPreferencesHelper.getLong(this, "CURRENT_THREAD_ID", -1L)
                if (lastThreadID != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val updatedRecord =
                            getSmsChatUserByThreadId(this@HomeActivity, lastThreadID)

                        withContext(Dispatchers.Main) {
                            updatedRecord?.let { record ->
                                val allMessagePosition =
                                    allMessageList.indexOfFirst { it.threadId == record.threadId }
                                if (allMessagePosition != -1) {
                                    val mutableList = allMessageList.toMutableList()
                                    mutableList[allMessagePosition] = record
                                    allMessageList = mutableList.toList()
                                }

                                if (messageFilter.contentEquals(getString(R.string.unread))) {
                                    val unreadMessageList =
                                        unReadMessagesFilterList.toMutableList()
                                    unreadMessageList.removeAll { it.threadId == record.threadId }
                                    unReadMessagesFilterList = unreadMessageList.toList()
                                    applyFilterOnMessageList(getString(R.string.unread))
                                }

                                rvMessageListAdapter.updateItemByThreadId(lastThreadID, record)
                            }
                        }
                    }
                }
                leftToRightSwipe =
                    SharedPreferencesHelper.getString(
                        this,
                        Const.RIGHT_SWIPE_ACTIONS,
                        resources.getString(R.string.delete)
                    )
                rightToLeftSwipe =
                    SharedPreferencesHelper.getString(
                        this,
                        Const.LEFT_SWIPE_ACTIONS,
                        resources.getString(R.string.archive)
                    )

                if (fontSize != SharedPreferencesHelper.getString(
                        this,
                        Const.FONT_SIZE,
                        getString(R.string.normal)
                    )
                ) {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finishAffinity()
                }

                if (leftToRightSwipe.contentEquals(resources.getString(R.string.none))) {
                    previousLeftToRightSwipe = leftToRightSwipe
                    disableSwipeLeftToRight()
                } else {
                    if (previousLeftToRightSwipe.contentEquals(resources.getString(R.string.none))) {
                        previousLeftToRightSwipe = leftToRightSwipe
                        setupLeftToRightSwipe()
                    }
                }

                if (rightToLeftSwipe.contentEquals(resources.getString(R.string.none))) {
                    previousRightToLeftSwipe = rightToLeftSwipe
                    disableSwipeRightToLeft()
                } else {
                    if (previousRightToLeftSwipe.contentEquals(resources.getString(R.string.none))) {
                        previousRightToLeftSwipe = rightToLeftSwipe
                        setupRightToLeftSwipe()
                    }
                }

                if (isLastProfileColorToChange != SharedPreferencesHelper.getBoolean(
                        this,
                        Const.IS_CHANGE_PROFILE_COLOR,
                        false
                    )
                ) {
                    isLastProfileColorToChange = SharedPreferencesHelper.getBoolean(
                        this,
                        Const.IS_CHANGE_PROFILE_COLOR,
                        false
                    )
                    rvMessageListAdapter.updateProfileColor()
                }
                rvMessageListAdapter.updateBlockContacts()

                val deletedThreadIds =
                    SharedPreferencesHelper.getDeletedThreadIds(this@HomeActivity)
                if (deletedThreadIds.isNotEmpty()) {
                    applyFilterOnMessageList(messageFilter)
                }
                afterApplyFilterUpdateMessagesCount(allMessageList)
            } else {
                afterEnablePermissionView()
                initView()
            }
        } else {
            disablePermissionView()

            if (isFirstTimeAskPermissions) {
                Handler(Looper.getMainLooper()).postDelayed({
                    isFirstTimeAskPermissions = false
                    if (shouldRequestSmsRole()) {
                        requestSmsRole()
                    }
                }, 300)
            }

            binding.btnSetDefaultPermission.setOnClickListener {
                if (shouldRequestSmsRole()) {
                    requestSmsRole()
                }
            }
        }
        super.onResume()
    }

    private fun disablePermissionView() {
        binding.paginationProgress.visibility = View.INVISIBLE
        binding.fabStartChat.visibility = View.INVISIBLE
        binding.rvDefaultPermissionView.visibility = View.VISIBLE
    }

    private fun afterEnablePermissionView() {
        binding.rvDefaultPermissionView.visibility = View.GONE
        binding.paginationProgress.visibility = View.VISIBLE
        binding.fabStartChat.visibility = View.VISIBLE
    }

    private fun shouldRequestSmsRole(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java) ?: return false
            return roleManager.isRoleAvailable(RoleManager.ROLE_SMS) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        }
        return false
    }

    private fun requestSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager: RoleManager = getSystemService(RoleManager::class.java) ?: return
            val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
            if (isRoleAvailable) {
                val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                if (!isRoleHeld) {
                    smsDefaultLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(
                    Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    packageName
                )
            }
            smsDefaultLauncher.launch(intent)
        }
    }

    private fun disableSwipeLeftToRight() {
        itemTouchHelperLeftToRight?.attachToRecyclerView(null)

        val rightSwipeCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return 0
            }
        }

        itemTouchHelperLeftToRight = ItemTouchHelper(rightSwipeCallback)
        itemTouchHelperLeftToRight?.attachToRecyclerView(binding.rvMessageList)
    }

    private fun disableSwipeRightToLeft() {
        itemTouchHelperRightToLeft?.attachToRecyclerView(null)

        val leftSwipeCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return 0
            }
        }

        itemTouchHelperRightToLeft = ItemTouchHelper(leftSwipeCallback)
        itemTouchHelperRightToLeft?.attachToRecyclerView(binding.rvMessageList)
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
            "${Telephony.Sms.DATE} DESC" // newest first
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
        val isPinned = SharedPreferencesHelper.isPinned(context, threadId)
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

    private fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrBlank()) return null

        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)
        if (normalizedNumber.isBlank()) return null

        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )

            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            ContactsContract.PhoneLookup.DISPLAY_NAME
                        )
                    )
                }
            }

            null
        } catch (_: IllegalArgumentException) {
            ""
        } catch (_: Exception) {
            ""
        }
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
                rvMessageListAdapter.updateMarkAsRead(threadId)
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
                rvMessageListAdapter.removeByThreadId(threadId)
                Handler(Looper.getMainLooper()).postDelayed({
                    clearSelectionViewOrUpdate()
                }, 100)
            }

            "BLOCKED" -> {
                val contactInfo =
                    getPhoneNumberOrAddressFromThreadId(
                        this@HomeActivity,
                        threadId
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