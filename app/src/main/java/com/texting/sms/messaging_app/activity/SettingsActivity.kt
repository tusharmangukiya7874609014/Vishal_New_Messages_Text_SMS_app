package com.texting.sms.messaging_app.activity

import android.app.Activity
import android.app.Dialog
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RadioButton
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivitySettingsBinding
import com.texting.sms.messaging_app.databinding.DialogDelaySendingMessageBinding
import com.texting.sms.messaging_app.databinding.DialogFontSizeBinding
import com.texting.sms.messaging_app.databinding.DialogOldMessageAutomaticallyBinding
import com.texting.sms.messaging_app.services.CallOverlayService
import com.texting.sms.messaging_app.utils.getColorFromAttr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings)
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
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@SettingsActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("SettingsActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SettingsActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SettingsActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@SettingsActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        loadAd(this@SettingsActivity)
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
        val delayedSendingMessage = SharedPreferencesHelper.getString(
            this, Const.DELAYED_SENDING_MESSAGE_TIMING, resources.getString(R.string.no_delay)
        )
        binding.txtDelaySendingMessage.text = delayedSendingMessage

        val isEnableAfterCallScreen =
            SharedPreferencesHelper.getBoolean(this, Const.ENABLE_AFTER_CALL_SCREEN, true)
        if (isEnableAfterCallScreen) {
            binding.switchEnableAfterCall.isChecked = true
            binding.switchEnableAfterCall.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))

            if (isFullScreenNotificationAllowed() || Settings.canDrawOverlays(this)) {
                if (!CallOverlayService.isRunning) {
                    val cmdIntent = Intent(this, CallOverlayService::class.java).apply {
                        putExtra("CALL_STATE", "100000")
                    }
                    startService(cmdIntent)
                }
            }
        } else {
            binding.switchEnableAfterCall.isChecked = false
            binding.switchEnableAfterCall.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
            stopService(Intent(this, CallOverlayService::class.java))
        }

        val isDeliveryConfirmations =
            SharedPreferencesHelper.getBoolean(this, Const.DELIVERY_CONFIRMATIONS, false)
        if (isDeliveryConfirmations) {
            binding.switchDeliveryConfirmations.isChecked = true
            binding.switchDeliveryConfirmations.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
        } else {
            binding.switchDeliveryConfirmations.isChecked = false
            binding.switchDeliveryConfirmations.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
        }

        val isUseSystemFont = SharedPreferencesHelper.useSystemFont(this)
        if (isUseSystemFont) {
            binding.switchUseSystemFont.isChecked = true
            binding.switchUseSystemFont.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
        } else {
            binding.switchUseSystemFont.isChecked = false
            binding.switchUseSystemFont.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
        }

        val isChangeProfileColor =
            SharedPreferencesHelper.getBoolean(this, Const.IS_CHANGE_PROFILE_COLOR, false)
        if (isChangeProfileColor) {
            binding.switchProfileColor.isChecked = true
            binding.switchProfileColor.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
        } else {
            binding.switchProfileColor.isChecked = false
            binding.switchProfileColor.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
        }

        val isMobileNumbersOnly =
            SharedPreferencesHelper.getBoolean(this, Const.IS_MOBILE_NUMBERS_ONLY, true)
        if (isMobileNumbersOnly) {
            binding.switchOnlyMobileNumbers.isChecked = true
            binding.switchOnlyMobileNumbers.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
        } else {
            binding.switchOnlyMobileNumbers.isChecked = false
            binding.switchOnlyMobileNumbers.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
        }

        val isSendingLongMessageAsMMS =
            SharedPreferencesHelper.getBoolean(this, Const.IS_SENDING_LONG_MESSAGE_AS_MMS, false)
        if (isSendingLongMessageAsMMS) {
            binding.switchLongMessages.isChecked = true
            binding.switchLongMessages.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
        } else {
            binding.switchLongMessages.isChecked = false
            binding.switchLongMessages.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
        }
    }

    private fun isFullScreenNotificationAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else true
    }

    private fun initClickListener() {
        binding.switchProfileColor.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.saveBoolean(this, Const.IS_CHANGE_PROFILE_COLOR, isChecked)
            if (isChecked) {
                binding.switchProfileColor.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                binding.switchProfileColor.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
            }
        }

        binding.switchUseSystemFont.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.setUseSystemFont(this, isChecked)
            if (isChecked) {
                binding.switchUseSystemFont.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                binding.switchUseSystemFont.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
            }
            Handler(Looper.getMainLooper()).postDelayed({
                recreate()
            }, 300)
        }

        binding.switchEnableAfterCall.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.saveBoolean(this, Const.ENABLE_AFTER_CALL_SCREEN, isChecked)
            if (isChecked) {
                binding.switchEnableAfterCall.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))

                if (isFullScreenNotificationAllowed() || Settings.canDrawOverlays(this)) {
                    if (!CallOverlayService.isRunning) {
                        val cmdIntent = Intent(this, CallOverlayService::class.java).apply {
                            putExtra("CALL_STATE", "100000")
                        }
                        startService(cmdIntent)
                    }
                }
            } else {
                binding.switchEnableAfterCall.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
                stopService(Intent(this, CallOverlayService::class.java))
            }
        }

        binding.switchDeliveryConfirmations.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.saveBoolean(this, Const.DELIVERY_CONFIRMATIONS, isChecked)
            if (isChecked) {
                binding.switchDeliveryConfirmations.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                binding.switchDeliveryConfirmations.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
            }
        }

        binding.switchOnlyMobileNumbers.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.saveBoolean(this, Const.IS_MOBILE_NUMBERS_ONLY, isChecked)
            if (isChecked) {
                binding.switchOnlyMobileNumbers.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                binding.switchOnlyMobileNumbers.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
            }
        }

        binding.switchLongMessages.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.saveBoolean(
                this,
                Const.IS_SENDING_LONG_MESSAGE_AS_MMS,
                isChecked
            )
            if (isChecked) {
                binding.switchLongMessages.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                binding.switchLongMessages.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
            }
        }

        binding.rvAppTheme.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            SharedPreferencesHelper.saveInt(this@SettingsActivity, "COUNT_CLICK", 0)
            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(Intent(this@SettingsActivity, AppThemeActivity::class.java))
                }
            } else {
                startActivity(Intent(this@SettingsActivity, AppThemeActivity::class.java))
            }
        }

        binding.rvFontSize.setOnClickListener {
            showFontSizeDialog()
        }

        binding.rvDelayedSending.setOnClickListener {
            showDelayedMessageSendingDialog()
        }

        binding.rvSwipeAction.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(
                        Intent(
                            this@SettingsActivity, SwipeActionsActivity::class.java
                        )
                    )
                }
            } else {
                startActivity(
                    Intent(
                        this@SettingsActivity, SwipeActionsActivity::class.java
                    )
                )
            }
        }

        binding.rvQuickMessage.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(
                        Intent(
                            this@SettingsActivity, QuickResponseActivity::class.java
                        )
                    )
                }
            } else {
                startActivity(
                    Intent(
                        this@SettingsActivity, QuickResponseActivity::class.java
                    )
                )
            }
        }

        binding.rvNotification.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(
                        Intent(
                            this@SettingsActivity, NotificationsActivity::class.java
                        )
                    )
                }
            } else {
                startActivity(
                    Intent(
                        this@SettingsActivity, NotificationsActivity::class.java
                    )
                )
            }
        }

        binding.rvDelete.setOnClickListener {
            showOldMessageDeleteDialog()
        }

        binding.rvRateUs.setOnClickListener {
            openPlayStoreForReview()
        }

        binding.rvShareApp.setOnClickListener {
            shareApp()
        }

        binding.rvChooseLanguage.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(Intent(this@SettingsActivity, ChangeLanguageActivity::class.java))
                }
            } else {
                startActivity(Intent(this@SettingsActivity, ChangeLanguageActivity::class.java))
            }
        }

        binding.ivBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.rvSyncMessages.setOnClickListener {
            if (binding.syncLoaderAnimation.isGone) {
                binding.syncLoaderAnimation.fadeIn()
                binding.syncLoaderAnimation.playAnimation()

                Handler(Looper.getMainLooper()).postDelayed({
                    binding.syncLoaderAnimation.cancelAnimation()
                    binding.syncLoaderAnimation.fadeOut()
                }, 12000)
            }
        }

        binding.rvAppearance.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(Intent(this@SettingsActivity, AppearanceActivity::class.java))
                }
            } else {
                startActivity(Intent(this@SettingsActivity, AppearanceActivity::class.java))
            }
        }

        binding.rvChatWallpaper.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@SettingsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@SettingsActivity) {
                    startActivity(
                        Intent(
                            this@SettingsActivity, ChatWallpaperActivity::class.java
                        )
                    )
                }
            } else {
                startActivity(
                    Intent(
                        this@SettingsActivity, ChatWallpaperActivity::class.java
                    )
                )
            }
        }

        binding.rvWidgetOptions.setOnClickListener {
            startActivity(Intent(this, WidgetActivity::class.java))
        }
    }

    private fun showDelayedMessageSendingDialog() {
        val dialog = Dialog(this)
        val delaySendingMessageBinding: DialogDelaySendingMessageBinding =
            DialogDelaySendingMessageBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(delaySendingMessageBinding.root)

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

        var delayedSendingMessage = SharedPreferencesHelper.getString(
            this, Const.DELAYED_SENDING_MESSAGE_TIMING, resources.getString(R.string.no_delay)
        )
        when (delayedSendingMessage) {
            resources.getString(R.string.no_delay) -> {
                delaySendingMessageBinding.rbNoDelay.isChecked = true
            }

            resources.getString(R.string._3_seconds) -> {
                delaySendingMessageBinding.rbThreeSeconds.isChecked = true
            }

            resources.getString(R.string._5_seconds) -> {
                delaySendingMessageBinding.rbFiveSeconds.isChecked = true
            }

            resources.getString(R.string._10_seconds) -> {
                delaySendingMessageBinding.rbTenSeconds.isChecked = true
            }
        }

        delaySendingMessageBinding.rgMessageDelaySelection.setOnCheckedChangeListener { _, checkedId ->
            val selectedValue = dialog.findViewById<RadioButton>(checkedId)
            if (selectedValue != null) {
                delayedSendingMessage = selectedValue.text.toString()
            }
        }

        delaySendingMessageBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        delaySendingMessageBinding.btnApply.setOnClickListener {
            dialog.dismiss()
            SharedPreferencesHelper.saveString(
                this, Const.DELAYED_SENDING_MESSAGE_TIMING, delayedSendingMessage
            )
            binding.txtDelaySendingMessage.text = delayedSendingMessage
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun showFontSizeDialog() {
        var selectedFontSize = resources.getString(R.string.normal)
        val dialog = Dialog(this)
        val fontSizeDialogBinding: DialogFontSizeBinding =
            DialogFontSizeBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(fontSizeDialogBinding.root)

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

        val fontSize = SharedPreferencesHelper.getString(
            this, Const.FONT_SIZE, resources.getString(R.string.normal)
        )
        when (fontSize) {
            resources.getString(R.string.small) -> {
                fontSizeDialogBinding.rbSmall.isChecked = true
            }

            resources.getString(R.string.normal) -> {
                fontSizeDialogBinding.rbNormal.isChecked = true
            }

            resources.getString(R.string.medium) -> {
                fontSizeDialogBinding.rbMedium.isChecked = true
            }

            resources.getString(R.string.large) -> {
                fontSizeDialogBinding.rbLarge.isChecked = true
            }
        }

        fontSizeDialogBinding.rgFontSelection.setOnCheckedChangeListener { _, checkedId ->
            val selectedValue = dialog.findViewById<RadioButton>(checkedId)
            if (selectedValue != null) {
                selectedFontSize = selectedValue.text.toString()
            }
        }

        fontSizeDialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        fontSizeDialogBinding.btnApply.setOnClickListener {
            dialog.dismiss()
            SharedPreferencesHelper.saveString(this, Const.FONT_SIZE, selectedFontSize)
            val scale = when (selectedFontSize) {
                resources.getString(R.string.small) -> 0.95f
                resources.getString(R.string.normal) -> 1.1f
                resources.getString(R.string.medium) -> 1.2f
                resources.getString(R.string.large) -> 1.3f
                else -> 1.1f
            }
            SharedPreferencesHelper.setFontScale(this, scale)
            Handler(Looper.getMainLooper()).postDelayed({
                restartActivity(this)
            }, 300)
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun restartActivity(activity: Activity) {
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }

    private fun showOldMessageDeleteDialog() {
        val dialog = Dialog(this)
        val oldMessageDeleteDialogBinding: DialogOldMessageAutomaticallyBinding =
            DialogOldMessageAutomaticallyBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(oldMessageDeleteDialogBinding.root)

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

        val oldMessageDeleteTime = SharedPreferencesHelper.getString(
            this, Const.OLD_MESSAGE_DELETE_TIME, resources.getString(R.string.never)
        )
        when (oldMessageDeleteTime) {
            resources.getString(R.string.never) -> {
                oldMessageDeleteDialogBinding.etNumberOfDays.text.clear()
            }

            else -> {
                oldMessageDeleteDialogBinding.etNumberOfDays.setText(oldMessageDeleteTime)
            }
        }

        oldMessageDeleteDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
            SharedPreferencesHelper.saveString(
                this, Const.OLD_MESSAGE_DELETE_TIME, resources.getString(R.string.never)
            )
            binding.txtOldMessageTiming.text = resources.getString(R.string.never)
        }

        oldMessageDeleteDialogBinding.btnYes.setOnClickListener {
            if (oldMessageDeleteDialogBinding.etNumberOfDays.text.isNotEmpty()) {
                dialog.dismiss()
                SharedPreferencesHelper.saveString(
                    this,
                    Const.OLD_MESSAGE_DELETE_TIME,
                    oldMessageDeleteDialogBinding.etNumberOfDays.text.toString()
                )
                binding.txtOldMessageTiming.text =
                    oldMessageDeleteDialogBinding.etNumberOfDays.text.toString()
            } else {
                showToast(getString(R.string.please_add_number_of_days))
            }
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Check out this amazing app: https://play.google.com/store/apps/details?id=$packageName"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
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
                    "https://play.google.com/store/apps/details?id=$packageName".toUri()
                )
            )
        }
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        binding.txtSelectedLanguage.text =
            SharedPreferencesHelper.getString(this, Const.SELECTED_LANGUAGE_NAME, "English")
        super.onResume()
    }


}