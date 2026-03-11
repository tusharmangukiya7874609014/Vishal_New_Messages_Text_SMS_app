package com.texting.sms.messaging_app.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ActivityAppereanceBinding
import com.texting.sms.messaging_app.adapter.ChatBoxColorAdapter
import com.texting.sms.messaging_app.adapter.ChatBoxStyleAdapter
import com.texting.sms.messaging_app.adapter.ChatWallpaperAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.listener.OnChatBoxClickInterface
import com.texting.sms.messaging_app.listener.OnChatBoxColorClickInterface
import com.texting.sms.messaging_app.listener.OnChatWallpaperClickInterface
import com.texting.sms.messaging_app.model.ChatBoxColor
import com.texting.sms.messaging_app.model.ChatBoxStyle
import com.texting.sms.messaging_app.model.ChatWallpaper
import com.texting.sms.messaging_app.utils.getColorFromAttr
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppearanceActivity : BaseActivity(), OnChatBoxClickInterface, OnChatBoxColorClickInterface,
    OnChatWallpaperClickInterface {
    private lateinit var binding: ActivityAppereanceBinding
    private lateinit var rvChatStyleBoxAdapter: ChatBoxStyleAdapter
    private lateinit var rvChatBoxColorAdapter: ChatBoxColorAdapter
    private lateinit var rvChatWallpaperAdapter: ChatWallpaperAdapter

    private var chatBoxLeftStyleList = mutableListOf<ChatBoxStyle>()
    private var chatBoxRightStyleList = mutableListOf<Int>()
    private var chatBoxColorList = mutableListOf<ChatBoxColor>()
    private var wallpaperList = mutableListOf<ChatWallpaper>()

    private val leftChatBubbleBoxStyle = listOf(
        R.drawable.ic_left_chat_box_1,
        R.drawable.ic_left_chat_box_2,
        R.drawable.ic_left_chat_box_3,
        R.drawable.ic_left_chat_box_4,
        R.drawable.ic_left_chat_box_5,
        R.drawable.ic_left_chat_box_6,
        R.drawable.ic_left_chat_box_7,
        R.drawable.ic_left_chat_box_8
    )
    private val rightChatBubbleBoxStyle = listOf(
        R.drawable.ic_right_chat_box_1,
        R.drawable.ic_right_chat_box_2,
        R.drawable.ic_right_chat_box_3,
        R.drawable.ic_right_chat_box_4,
        R.drawable.ic_right_chat_box_5,
        R.drawable.ic_right_chat_box_6,
        R.drawable.ic_right_chat_box_7,
        R.drawable.ic_right_chat_box_8
    )

    private val darkThemeChatBubbleColors = listOf(
        R.color.dark_chat_default_color,
        R.color.dark_chat_box_color_1,
        R.color.dark_chat_box_color_2,
        R.color.dark_chat_box_color_3,
        R.color.dark_chat_box_color_4,
        R.color.dark_chat_box_color_5,
        R.color.dark_chat_box_color_6,
        R.color.dark_chat_box_color_7,
        R.color.dark_chat_box_color_8,
        R.color.dark_chat_box_color_9,
        R.color.dark_chat_box_color_10,
        R.color.dark_chat_box_color_11,
        R.color.dark_chat_box_color_12,
        R.color.dark_chat_box_color_13,
        R.color.dark_chat_box_color_14,
        R.color.dark_chat_box_color_15,
        R.color.dark_chat_box_color_16,
        R.color.dark_chat_box_color_17,
        R.color.dark_chat_box_color_18,
        R.color.dark_chat_box_color_19,
        R.color.dark_chat_box_color_20,
        R.color.dark_chat_box_color_21,
        R.color.dark_chat_box_color_22,
        R.color.dark_chat_box_color_23,
        R.color.dark_chat_box_color_24,
        R.color.dark_chat_box_color_25,
        R.color.dark_chat_box_color_26,
        R.color.dark_chat_box_color_27,
        R.color.dark_chat_box_color_28
    )
    private val lightThemeChatBubbleColors = listOf(
        R.color.light_chat_default_color,
        R.color.light_chat_box_color_1,
        R.color.light_chat_box_color_2,
        R.color.light_chat_box_color_3,
        R.color.light_chat_box_color_4,
        R.color.light_chat_box_color_5,
        R.color.light_chat_box_color_6,
        R.color.light_chat_box_color_7,
        R.color.light_chat_box_color_8,
        R.color.light_chat_box_color_9,
        R.color.light_chat_box_color_10,
        R.color.light_chat_box_color_11,
        R.color.light_chat_box_color_12,
        R.color.light_chat_box_color_13,
        R.color.light_chat_box_color_14,
        R.color.light_chat_box_color_15,
        R.color.light_chat_box_color_16,
        R.color.light_chat_box_color_17,
        R.color.light_chat_box_color_18,
        R.color.light_chat_box_color_19,
        R.color.light_chat_box_color_20,
        R.color.light_chat_box_color_21,
        R.color.light_chat_box_color_22,
        R.color.light_chat_box_color_23,
        R.color.light_chat_box_color_24,
        R.color.light_chat_box_color_25,
        R.color.light_chat_box_color_26,
        R.color.light_chat_box_color_27,
        R.color.light_chat_box_color_28
    )

    private val darkColorOfWallpapers = listOf(
        R.color.dark_chat_theme_1,
        R.color.dark_chat_theme_2,
        R.color.dark_chat_theme_3,
        R.color.dark_chat_theme_4,
        R.color.dark_chat_theme_5,
        R.color.dark_chat_theme_6,
        R.color.dark_chat_theme_7,
        R.color.dark_chat_theme_8,
        R.color.dark_chat_theme_9,
        R.color.dark_chat_theme_10,
        R.color.dark_chat_theme_11,
        R.color.dark_chat_theme_12,
        R.color.dark_chat_theme_13,
        R.color.dark_chat_theme_14,
        R.color.dark_chat_theme_15,
        R.color.dark_chat_theme_16,
        R.color.dark_chat_theme_17,
        R.color.dark_chat_theme_18,
        R.color.dark_chat_theme_19,
        R.color.dark_chat_theme_20,
        R.color.dark_chat_theme_21,
        R.color.dark_chat_theme_22,
        R.color.dark_chat_theme_23,
        R.color.dark_chat_theme_24,
        R.color.dark_chat_theme_25,
        R.color.dark_chat_theme_26,
        R.color.dark_chat_theme_27,
        R.color.dark_chat_theme_28,
        R.color.dark_chat_theme_29,
        R.color.dark_chat_theme_30,
        R.color.dark_chat_theme_31
    )
    private val lightColorOfWallpapers = listOf(
        R.color.light_chat_theme_1,
        R.color.light_chat_theme_2,
        R.color.light_chat_theme_3,
        R.color.light_chat_theme_4,
        R.color.light_chat_theme_5,
        R.color.light_chat_theme_6,
        R.color.light_chat_theme_7,
        R.color.light_chat_theme_8,
        R.color.light_chat_theme_9,
        R.color.light_chat_theme_10,
        R.color.light_chat_theme_11,
        R.color.light_chat_theme_12,
        R.color.light_chat_theme_13,
        R.color.light_chat_theme_14,
        R.color.light_chat_theme_15,
        R.color.light_chat_theme_16,
        R.color.light_chat_theme_17,
        R.color.light_chat_theme_18,
        R.color.light_chat_theme_19,
        R.color.light_chat_theme_20,
        R.color.light_chat_theme_21,
        R.color.light_chat_theme_22,
        R.color.light_chat_theme_23,
        R.color.light_chat_theme_24,
        R.color.light_chat_theme_25,
        R.color.light_chat_theme_26,
        R.color.light_chat_theme_27,
        R.color.light_chat_theme_28,
        R.color.light_chat_theme_29,
        R.color.light_chat_theme_30,
        R.color.light_chat_theme_31
    )

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var onPermissionGranted: (() -> Unit)? = null
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_appereance)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                onPermissionGranted?.invoke()
            } else {
                showToast(getString(R.string.permission_denied))
            }
        }
        runAdsCampion()
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@AppearanceActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AppearanceActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AppearanceActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AppearanceActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@AppearanceActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("AppearanceActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AppearanceActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AppearanceActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AppearanceActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        loadAd(this@AppearanceActivity)
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
        binding.apply {
            selectedTab = 1
        }

        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    imageUri = result.data?.data
                    val intent = Intent(this, PreviewWallpaperActivity::class.java)
                    intent.putExtra("imageUri", imageUri.toString())
                    startActivity(intent)
                }
            }

        /**  Initialize chat box style  **/
        lifecycleScope.launch(Dispatchers.IO) {
            setupChatBoxStyleRecycler()
            buildChatBoxStyleList()

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                /**  Set previous chat box style  **/
                val chatBoxStylePosition =
                    SharedPreferencesHelper.getInt(
                        this@AppearanceActivity,
                        Const.CHAT_BOX_STYLE_POSITION,
                        0
                    )

                if (chatBoxStylePosition in chatBoxLeftStyleList.indices) {
                    chatBoxLeftStyleList[chatBoxStylePosition].chatBox.let {
                        if (it != null) {
                            binding.rvChatLeftBubbleOne.background =
                                ContextCompat.getDrawable(this@AppearanceActivity, it)
                            binding.rvChatLeftBubbleTwo.background =
                                ContextCompat.getDrawable(this@AppearanceActivity, it)
                            binding.rvChatLeftBubbleThree.background =
                                ContextCompat.getDrawable(this@AppearanceActivity, it)
                        }
                    }
                }

                if (chatBoxStylePosition in chatBoxRightStyleList.indices) {
                    chatBoxRightStyleList[chatBoxStylePosition].let {
                        binding.rvChatRightBubbleOne.background =
                            ContextCompat.getDrawable(this@AppearanceActivity, it)
                        binding.rvChatRightBubbleTwo.background =
                            ContextCompat.getDrawable(this@AppearanceActivity, it)
                    }
                }
            }
        }

        /**  Initialize chat box color   **/
        lifecycleScope.launch(Dispatchers.IO) {
            setupChatBoxColorRecycler()
            buildChatBoxColorList()

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                /**  Set previous chat box color  **/
                val chatBoxColorPosition =
                    SharedPreferencesHelper.getInt(
                        this@AppearanceActivity,
                        Const.CHAT_BOX_COLOR_POSITION,
                        0
                    )

                if (chatBoxColorPosition in chatBoxColorList.indices) {
                    if (SharedPreferencesHelper.getBoolean(
                            this@AppearanceActivity,
                            Const.IS_ME_CHAT_BOX_COLOR,
                            true
                        )
                    ) {
                        chatBoxColorList[chatBoxColorPosition].chatBoxColor.let {
                            if (it != null) binding.rightChatBoxColor =
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@AppearanceActivity,
                                        it
                                    )
                                )
                        }

                        binding.leftChatBoxColor =
                            ColorStateList.valueOf(getColorFromAttr(R.attr.chatBoxColor))
                    } else {
                        chatBoxColorList[chatBoxColorPosition].chatBoxColor.let {
                            if (it != null) binding.leftChatBoxColor =
                                ColorStateList.valueOf(
                                    ContextCompat.getColor(
                                        this@AppearanceActivity,
                                        it
                                    )
                                )
                        }

                        binding.rightChatBoxColor =
                            ColorStateList.valueOf(getColorFromAttr(R.attr.chatBoxColor))
                    }
                }
            }
        }

        /**  Initialize chat Wallpaper  **/
        lifecycleScope.launch(Dispatchers.IO) {
            setupWallpaperRecycler()
            buildChatWallpaperList()

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                /**  Set previous chat Wallpaper  **/
                val wallpaperType = SharedPreferencesHelper.getString(
                    this@AppearanceActivity,
                    Const.WALLPAPER_TYPE,
                    "Default"
                )

                when (wallpaperType) {
                    "Default" -> {
                        binding.chatDesign.setImageDrawable(null)
                        binding.rvChangeColor.setBackgroundColor(getColorFromAttr(R.attr.mainBackground))
                    }

                    "Others" -> {
                        if (::rvChatWallpaperAdapter.isInitialized && wallpaperList.isNotEmpty()) {
                            val selectedPosition =
                                SharedPreferencesHelper.getInt(
                                    this@AppearanceActivity,
                                    Const.OTHERS_WALLPAPER_POSITION,
                                    -1
                                )

                            Glide.with(this@AppearanceActivity)
                                .load(
                                    ContextCompat.getDrawable(
                                        this@AppearanceActivity,
                                        R.drawable.img_chat_design_view
                                    )
                                )
                                .into(binding.chatDesign)

                            if (selectedPosition in wallpaperList.indices) {
                                wallpaperList[selectedPosition].chatWallpaper.let {
                                    if (it != null) binding.rvChangeColor.setBackgroundColor(
                                        ContextCompat.getColor(this@AppearanceActivity, it)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupChatBoxStyleRecycler() {
        rvChatStyleBoxAdapter = ChatBoxStyleAdapter(mutableListOf(), this, this)

        binding.rvChatBoxStyle.apply {
            adapter = rvChatStyleBoxAdapter
            layoutManager = object : GridLayoutManager(this@AppearanceActivity, 2) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun buildChatBoxStyleList() {
        if (chatBoxLeftStyleList.isNotEmpty() && chatBoxRightStyleList.isNotEmpty()) return

        chatBoxLeftStyleList.addAll(leftChatBubbleBoxStyle.map { ChatBoxStyle(it) })
        chatBoxRightStyleList.addAll(rightChatBubbleBoxStyle.map { it })

        rvChatStyleBoxAdapter.updateData(chatBoxLeftStyleList)
    }

    private fun setupChatBoxColorRecycler() {
        rvChatBoxColorAdapter = ChatBoxColorAdapter(mutableListOf(), this, this)

        binding.rvChatBoxColor.apply {
            adapter = rvChatBoxColorAdapter
            layoutManager = GridLayoutManager(this@AppearanceActivity, 5)
            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun buildChatBoxColorList() {
        if (chatBoxColorList.isNotEmpty()) return

        val colors = if (isDarkTheme()) {
            darkThemeChatBubbleColors
        } else {
            lightThemeChatBubbleColors
        }

        val chatBoxColors = colors.map { colorRes ->
            ChatBoxColor(colorRes)
        }

        chatBoxColorList.addAll(chatBoxColors)
        rvChatBoxColorAdapter.updateData(chatBoxColors)
    }

    private fun setupWallpaperRecycler() {
        binding.rvChatWallpaper.apply {
            layoutManager = LinearLayoutManager(
                this@AppearanceActivity, LinearLayoutManager.HORIZONTAL, false
            )
            setHasFixedSize(true)
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }

        rvChatWallpaperAdapter = ChatWallpaperAdapter(
            mutableListOf(), this, this
        )
        binding.rvChatWallpaper.adapter = rvChatWallpaperAdapter
    }

    private fun buildChatWallpaperList() {
        if (wallpaperList.isNotEmpty()) return

        val sourceList = if (isDarkTheme()) darkColorOfWallpapers else lightColorOfWallpapers

        val wallpapers = sourceList.map { colorRes ->
            ChatWallpaper(colorRes)
        }

        wallpaperList.addAll(wallpapers)
        rvChatWallpaperAdapter.updateData(wallpapers)
    }

    private val permissionRequestLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                showToast(getString(R.string.permission_denied))
            }
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        imagePickerLauncher.launch(intent)
    }

    private fun initClickListener() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val adsEnabled = SharedPreferencesHelper.getBoolean(
                    this@AppearanceActivity, Const.IS_ADS_ENABLED, false
                )
                val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                    this@AppearanceActivity, Const.IS_INTERSTITIAL_ENABLED, false
                )

                if (adsEnabled && interstitialEnabled) {
                    InterstitialAdHelper.showAd(this@AppearanceActivity) {
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

        binding.rvStyleView.setOnClickListener {
            binding.selectedTab = 1
            binding.rvChatBoxColor.visibility = View.GONE
            binding.llChatWallpaper.visibility = View.GONE
            binding.ivSwapColorChange.visibility = View.INVISIBLE
            binding.rvChatBoxStyle.fadeIn()
        }

        binding.rvColorView.setOnClickListener {
            binding.selectedTab = 2

            binding.rvChatBoxStyle.visibility = View.GONE
            binding.llChatWallpaper.visibility = View.GONE
            binding.ivSwapColorChange.visibility = View.VISIBLE
            binding.rvChatBoxColor.fadeIn()
        }

        binding.rvChatWallpaperView.setOnClickListener {
            binding.selectedTab = 3

            binding.rvChatBoxStyle.visibility = View.GONE
            binding.rvChatBoxColor.visibility = View.GONE
            binding.ivSwapColorChange.visibility = View.INVISIBLE
            binding.llChatWallpaper.fadeIn()

            val wallpaperType =
                SharedPreferencesHelper.getString(this, Const.WALLPAPER_TYPE, "Default")
            if (wallpaperType.contentEquals("Others")) {
                binding.rvChatWallpaper.post {
                    binding.rvChatWallpaper.smoothScrollToPosition(
                        SharedPreferencesHelper.getInt(
                            this, Const.OTHERS_WALLPAPER_POSITION, -1
                        )
                    )
                }
            }
        }

        binding.rvAppDefaultWallpaper.setOnClickListener {
            binding.chatDesign.setImageDrawable(null)
            binding.rvChangeColor.setBackgroundColor(getColorFromAttr(R.attr.mainBackground))
            SharedPreferencesHelper.saveString(this, Const.WALLPAPER_TYPE, "Default")
            SharedPreferencesHelper.saveInt(this, Const.OTHERS_WALLPAPER_POSITION, -1)
            if (::rvChatWallpaperAdapter.isInitialized) {
                rvChatWallpaperAdapter.updateSelectedChatWallpaper(-1)
            }
        }

        binding.rvChooseFromGallary.setOnClickListener {
            checkAndRequestPermission()
        }

        binding.ivSwapColorChange.setOnClickListener {
            val chatBoxColorPosition =
                SharedPreferencesHelper.getInt(this, Const.CHAT_BOX_COLOR_POSITION, 0)

            if (SharedPreferencesHelper.getBoolean(this, Const.IS_ME_CHAT_BOX_COLOR, true)) {
                if (chatBoxColorPosition in chatBoxColorList.indices) {
                    chatBoxColorList[chatBoxColorPosition].chatBoxColor.let {
                        if (it != null) binding.leftChatBoxColor =
                            ColorStateList.valueOf(ContextCompat.getColor(this, it))
                    }
                }

                binding.rightChatBoxColor =
                    ColorStateList.valueOf(getColorFromAttr(R.attr.chatBoxColor))
                SharedPreferencesHelper.saveBoolean(this, Const.IS_ME_CHAT_BOX_COLOR, false)
            } else {
                if (chatBoxColorPosition in chatBoxColorList.indices) {
                    chatBoxColorList[chatBoxColorPosition].chatBoxColor.let {
                        if (it != null) binding.rightChatBoxColor =
                            ColorStateList.valueOf(ContextCompat.getColor(this, it))
                    }
                }

                binding.leftChatBoxColor =
                    ColorStateList.valueOf(getColorFromAttr(R.attr.chatBoxColor))
                SharedPreferencesHelper.saveBoolean(this, Const.IS_ME_CHAT_BOX_COLOR, true)
            }
        }
    }

    override fun onResume() {
        val wallpaperType = SharedPreferencesHelper.getString(this, Const.WALLPAPER_TYPE, "Default")

        when (wallpaperType) {
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
                            .diskCacheStrategy(DiskCacheStrategy.ALL).skipMemoryCache(false)
                            .into(binding.chatDesign)
                    } else {
                        Glide.with(this).load(imageUri).diskCacheStrategy(DiskCacheStrategy.ALL)
                            .skipMemoryCache(false).into(binding.chatDesign)
                    }

                    SharedPreferencesHelper.saveInt(this, Const.OTHERS_WALLPAPER_POSITION, -1)
                    if (::rvChatWallpaperAdapter.isInitialized) {
                        rvChatWallpaperAdapter.updateSelectedChatWallpaper(-1)
                    }
                } else {
                    binding.chatDesign.setImageDrawable(null)
                    binding.rvChangeColor.setBackgroundColor(getColorFromAttr(R.attr.mainBackground))
                }
            }
        }
        super.onResume()
    }

    override fun onSelectChatBoxClick(
        position: Int, chatBoxStyle: ChatBoxStyle
    ) {
        SharedPreferencesHelper.saveInt(this, Const.CHAT_BOX_STYLE_POSITION, position)
        chatBoxStyle.chatBox.let {
            if (it != null) {
                binding.rvChatLeftBubbleOne.background = ContextCompat.getDrawable(this, it)
                binding.rvChatLeftBubbleTwo.background = ContextCompat.getDrawable(this, it)
                binding.rvChatLeftBubbleThree.background = ContextCompat.getDrawable(this, it)
            }
        }

        if (position in chatBoxRightStyleList.indices) {
            chatBoxRightStyleList[position].let {
                binding.rvChatRightBubbleOne.background = ContextCompat.getDrawable(this, it)
                binding.rvChatRightBubbleTwo.background = ContextCompat.getDrawable(this, it)
            }
        }
        rvChatStyleBoxAdapter.updateSelectedChatBoxStyle(position)
    }

    override fun onSelectChatBoxColorClick(
        position: Int, chatBoxColor: ChatBoxColor
    ) {
        SharedPreferencesHelper.saveInt(this, Const.CHAT_BOX_COLOR_POSITION, position)
        if (SharedPreferencesHelper.getBoolean(this, Const.IS_ME_CHAT_BOX_COLOR, true)) {
            chatBoxColor.chatBoxColor.let {
                if (it != null) {
                    binding.rightChatBoxColor =
                        ColorStateList.valueOf(ContextCompat.getColor(this, it))
                }
            }
            binding.leftChatBoxColor = ColorStateList.valueOf(getColorFromAttr(R.attr.chatBoxColor))
        } else {
            chatBoxColor.chatBoxColor.let {
                if (it != null) {
                    binding.leftChatBoxColor =
                        ColorStateList.valueOf(ContextCompat.getColor(this, it))
                }
            }
            binding.rightChatBoxColor =
                ColorStateList.valueOf(getColorFromAttr(R.attr.chatBoxColor))
        }
        rvChatBoxColorAdapter.updateSelectedChatBoxColor(position)
    }

    override fun onChatWallpaperClick(
        position: Int, color: ChatWallpaper
    ) {
        Glide.with(this).load(ContextCompat.getDrawable(this, R.drawable.img_chat_design_view))
            .into(binding.chatDesign)
        color.chatWallpaper.let {
            if (it != null) binding.rvChangeColor.setBackgroundColor(
                ContextCompat.getColor(this, it)
            )
        }

        SharedPreferencesHelper.saveString(this, Const.WALLPAPER_TYPE, "Others")
        SharedPreferencesHelper.saveInt(this, Const.OTHERS_WALLPAPER_POSITION, position)

        rvChatWallpaperAdapter.updateSelectedChatWallpaper(position)
    }
}