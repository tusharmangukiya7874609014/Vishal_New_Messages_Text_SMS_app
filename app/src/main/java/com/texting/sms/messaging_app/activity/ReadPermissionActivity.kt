package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
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
import com.texting.sms.messaging_app.databinding.ActivityReadPermissionBinding
import com.texting.sms.messaging_app.databinding.DialogPermissionDeniedBinding
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadPermissionActivity : BaseActivity(), NetworkAvailableListener {
    private lateinit var binding: ActivityReadPermissionBinding

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_read_permission)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebaseLogEvent(
            this@ReadPermissionActivity, "READ_PERMISSION_PAGE", "READ_PERMISSION_SHOWN"
        )
        applySpannableTerms()
        initClickListener()
    }

    private fun showPermissionsDeniedDialog() {
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

        permissionDeniedDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        permissionDeniedDialogBinding.btnSettings.setOnClickListener {
            dialog.dismiss()
            openAppSettings()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                firebaseLogEvent(
                    this@ReadPermissionActivity, "READ_PERMISSION_PAGE", "NOTIFICATION_ALLOWED"
                )

                SharedPreferencesHelper.saveInt(this, "NOTIFICATION_PERMISSION_DENIED", 0)
                requestPhonePermissions()
            } else {
                val deniedCount =
                    (SharedPreferencesHelper.getInt(this, "NOTIFICATION_PERMISSION_DENIED", 0) + 1)

                SharedPreferencesHelper.saveInt(this, "NOTIFICATION_PERMISSION_DENIED", deniedCount)

                if (deniedCount >= 2) {
                    showPermissionsDeniedDialog()
                } else {
                    showToast(getString(R.string.notification_permission_denied))
                }
            }
        }

    private val phonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                if (!binding.checkPrivacyPolicy.isChecked) {
                    showToast(
                        getString(R.string.please_accept_the_privacy_policy_and_terms_conditions)
                    )
                    return@registerForActivityResult
                }

                SharedPreferencesHelper.saveInt(this, "READ_PHONE_STATE_PERMISSION_DENIED", 0)

                val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
                    this,
                    "IS_FIRST_TIME_LANGUAGE_SELECTED",
                    false
                )

                val nextIntent = when {
                    !isOverlayPermissionGranted() -> {
                        Intent(this, OverlayPermissionActivity::class.java)
                    }

                    !isFirstTimeLanguageSelected -> {
                        Intent(this, LanguageActivity::class.java)
                    }

                    else -> {
                        Intent(this, HomeActivity::class.java)
                    }
                }

                startActivity(nextIntent)
                finish()
            } else {
                val deniedCount = (SharedPreferencesHelper.getInt(
                    this,
                    "READ_PHONE_STATE_PERMISSION_DENIED",
                    0
                ) + 1)
                SharedPreferencesHelper.saveInt(
                    this,
                    "READ_PHONE_STATE_PERMISSION_DENIED",
                    deniedCount
                )

                if (deniedCount >= 2) {
                    showPermissionsDeniedDialog()
                } else {
                    showToast(getString(R.string.phone_permissions_denied))
                }
            }
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPhonePermissions()
            }
        } else {
            requestPhonePermissions()
        }
    }

    private fun requestPhonePermissions() {
        val phonePermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE
        )

        val notGranted = phonePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            phonePermissionLauncher.launch(notGranted.toTypedArray())
        } else {
            firebaseLogEvent(
                this@ReadPermissionActivity, "READ_PERMISSION_PAGE", "MAKE_PHONE_STATE_ALLOWED"
            )

            if (!binding.checkPrivacyPolicy.isChecked) {
                showToast(
                    getString(R.string.please_accept_the_privacy_policy_and_terms_conditions)
                )
                return
            }

            val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
                this,
                "IS_FIRST_TIME_LANGUAGE_SELECTED",
                false
            )

            val nextIntent = when {
                !isOverlayPermissionGranted() -> {
                    Intent(this, OverlayPermissionActivity::class.java)
                }

                !isFirstTimeLanguageSelected -> {
                    Intent(this, LanguageActivity::class.java)
                }

                else -> {
                    Intent(this, HomeActivity::class.java)
                }
            }

            startActivity(nextIntent)
            finish()
        }
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun applySpannableTerms() {
        val privacyText = "Privacy policy"
        val fullText = "I agree to the $privacyText of App"

        val spannable = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = SharedPreferencesHelper.getString(
                    this@ReadPermissionActivity,
                    Const.PRIVACY_URL,
                    ""
                ).ifEmpty { "https://sites.google.com/view/sms-text-privacy-policy/home" }

                if (url.isNotBlank()) {
                    openCustomTabSafely(url)
                } else {
                    showToast(getString(R.string.something_went_wrong))
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(
                    this@ReadPermissionActivity,
                    R.color.app_theme_color
                )
            }
        }

        val start = fullText.indexOf(privacyText)
        if (start >= 0) {
            spannable.setSpan(
                clickableSpan,
                start,
                start + privacyText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.txtPrivacyPolicy.apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    private fun openCustomTabSafely(url: String) {
        try {
            val params = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(
                    ContextCompat.getColor(this, R.color.app_theme_color)
                )
                .build()

            val intent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(params)
                .setShowTitle(true)
                .build()

            intent.launchUrl(this, url.toUri())

        } catch (_: Exception) {
            openInBrowserFallback(url)
        }
    }

    private fun openInBrowserFallback(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            showToast(getString(R.string.no_browser_found))
        }
    }

    private fun initClickListener() {
        binding.btnAllowPermission.setOnClickListener {
            firebaseLogEvent(
                this@ReadPermissionActivity, "READ_PERMISSION_PAGE", "TAP_TO_ALLOW_PERMISSION"
            )

            requestNotificationPermission()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
            getString(R.string.enable_required_features_to_use_messaging_your_data_stays_private_and_secure_on_your_device)

        permissionDeniedDialogBinding.btnSettings.text = getString(R.string.allow)

        permissionDeniedDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        permissionDeniedDialogBinding.btnSettings.setOnClickListener {
            dialog.dismiss()

            requestNotificationPermission()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@ReadPermissionActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ReadPermissionActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ReadPermissionActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@ReadPermissionActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("ReadPermissionActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ReadPermissionActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ReadPermissionActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ReadPermissionActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

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
                        bannerAdsId = currentPageBannerAdsID
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
        bannerAdsId: String
    ) {
        binding.bannerAdContainer.iMediumRectangleShimmer.root.visibility = View.GONE
        binding.bannerAdContainer.iAdaptiveShimmer.root.visibility = View.VISIBLE

        binding.bannerAdContainer.root.visibility = View.VISIBLE
        BannerAdHelper.loadBannerAd(
            this,
            binding.bannerAdContainer.bannerAdView,
            binding.bannerAdContainer.shimmerViewContainer,
            bannerType = BannerType.ADAPTIVE,
            view = binding.bannerAdContainer.view,
            bannerAdsId = bannerAdsId
        )
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