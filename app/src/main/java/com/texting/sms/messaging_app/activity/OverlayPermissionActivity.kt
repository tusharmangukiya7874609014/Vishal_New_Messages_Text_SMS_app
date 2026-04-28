package com.texting.sms.messaging_app.activity

import android.app.Dialog
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityOverlayPermissionBinding
import com.texting.sms.messaging_app.databinding.DialogPermissionDeniedBinding
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.services.CallOverlayService
import com.texting.sms.messaging_app.services.OverlayPermissionMonitorService
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayPermissionActivity : BaseActivity(), NetworkAvailableListener {
    private lateinit var binding: ActivityOverlayPermissionBinding
    private var isAskForPermissions = false

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overlay_permission)
        firebaseCustomEvent(
            this@OverlayPermissionActivity,
            "display_overlay_visible",
            "overlay_permission_page",
            "open"
        )
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
                this@OverlayPermissionActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@OverlayPermissionActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@OverlayPermissionActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@OverlayPermissionActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("OverlayPermissionActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@OverlayPermissionActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@OverlayPermissionActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@OverlayPermissionActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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

    private fun initClickListener() {
        binding.btnAllowPermission.setOnClickListener {
            firebaseCustomEvent(
                this@OverlayPermissionActivity,
                "click_enable_overlay_btn",
                "overlay_permission_page",
                "tap_button"
            )

            requestOverlayAndFullScreenNotificationsPermission()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                firebaseCustomEvent(
                    this@OverlayPermissionActivity,
                    "back_click_from_overlay",
                    "overlay_permission_page",
                    "back_click"
                )

                showPermissionRequiredInformationDialog()
            }
        })
    }

    private fun showPermissionRequiredInformationDialog() {
        val dialog = Dialog(this)
        val permissionDeniedDialogBinding: DialogPermissionDeniedBinding =
            DialogPermissionDeniedBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(permissionDeniedDialogBinding.root)

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

        permissionDeniedDialogBinding.txtStatement.text =
            getString(R.string.this_permission_is_required_to_after_call_features_please_allow_this_permission)

        permissionDeniedDialogBinding.btnSettings.text = getString(R.string.allow)

        permissionDeniedDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        permissionDeniedDialogBinding.btnSettings.setOnClickListener {
            dialog.dismiss()

            requestOverlayAndFullScreenNotificationsPermission()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun requestOverlayAndFullScreenNotificationsPermission() {
        if (!Settings.canDrawOverlays(this)) {
            if (!isFullScreenNotificationAllowed()) {
                openFullScreenSettings()
            }

            openOverlaySettings()

            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(
                    Intent(
                        this@OverlayPermissionActivity,
                        PermissionOverlayActivity::class.java
                    )
                )
            }, 500)
        }
    }

    private fun openOverlaySettings() {
        isAskForPermissions = true
        val serviceIntent = Intent(this, OverlayPermissionMonitorService::class.java)
        startService(serviceIntent)

        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:$packageName".toUri()
        )
        startActivity(intent)
    }

    private fun openFullScreenSettings() {
        if (Build.VERSION.SDK_INT >= 34) {
            SharedPreferencesHelper.saveBoolean(this, "IS_FULL_SCREEN_NOTIFICATION", true)
            val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
        }
    }

    private fun isFullScreenNotificationAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else true
    }

    override fun onResume() {
        if (isAskForPermissions) {
            val isEnablePostCallScreen =
                SharedPreferencesHelper.getBoolean(this, Const.ENABLE_AFTER_CALL_SCREEN, true)

            if (Settings.canDrawOverlays(this)) {
                firebaseCustomEvent(
                    this@OverlayPermissionActivity,
                    "overlay_permission_allowed",
                    "overlay_permission_page",
                    "allowed"
                )
            } else {
                firebaseCustomEvent(
                    this@OverlayPermissionActivity,
                    "overlay_permission_denied",
                    "overlay_permission_page",
                    "denied"
                )
            }

            if (isFullScreenNotificationAllowed()) {
                firebaseCustomEvent(
                    this@OverlayPermissionActivity,
                    "full_notification_allowed",
                    "overlay_permission_page",
                    "allowed"
                )
            }

            if (isFullScreenNotificationAllowed() || Settings.canDrawOverlays(this)) {
                if (isEnablePostCallScreen && !CallOverlayService.isRunning) {
                    if (!CallOverlayService.isRunning) {
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
            }

            val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
                this@OverlayPermissionActivity,
                "IS_FIRST_TIME_LANGUAGE_SELECTED",
                false
            )

            val intent = if (!isFirstTimeLanguageSelected) {
                Intent(this@OverlayPermissionActivity, LanguageActivity::class.java)
            } else {
                Intent(this@OverlayPermissionActivity, HomeActivity::class.java)
            }

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
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