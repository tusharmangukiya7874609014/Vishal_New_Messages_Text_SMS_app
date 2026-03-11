package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityNotificationsBinding
import com.texting.sms.messaging_app.databinding.DialogNotificationButtonViewBinding
import com.texting.sms.messaging_app.databinding.DialogNotificationsPreviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationsActivity : BaseActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private var notificationPreview = Const.STRING_DEFAULT_VALUE
    private var notificationButtonOne = Const.STRING_DEFAULT_VALUE
    private var notificationButtonTwo = Const.STRING_DEFAULT_VALUE
    private var notificationButtonThree = Const.STRING_DEFAULT_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notifications)
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
                this@NotificationsActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@NotificationsActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@NotificationsActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@NotificationsActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@NotificationsActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("NotificationsActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@NotificationsActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@NotificationsActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@NotificationsActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        loadAd(this@NotificationsActivity)
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

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openNotificationSettings(this)
            } else {
                showToast(resources.getString(R.string.permission_denied))
            }
        }

    private fun initView() {
        notificationPreview = SharedPreferencesHelper.getString(
            this,
            Const.NOTIFICATIONS_PREVIEW,
            resources.getString(R.string.show_name_message)
        )
        notificationButtonOne = SharedPreferencesHelper.getString(
            this,
            Const.NOTIFICATION_BUTTON_ONE,
            resources.getString(R.string.none)
        )
        notificationButtonTwo = SharedPreferencesHelper.getString(
            this,
            Const.NOTIFICATION_BUTTON_TWO,
            resources.getString(R.string.mark_read)
        )
        notificationButtonThree = SharedPreferencesHelper.getString(
            this,
            Const.NOTIFICATION_BUTTON_THREE,
            resources.getString(R.string.reply)
        )
        binding.txtNotificationPreviewValue.text = notificationPreview
        binding.txtButtonOne.text = notificationButtonOne
        binding.txtButtonTwo.text = notificationButtonTwo
        binding.txtButtonThree.text = notificationButtonThree
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings(this)
            }
        } else {
            openNotificationSettings(this)
        }
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvNotification.setOnClickListener {
            askNotificationPermission()
        }

        binding.rvNotificationPreviews.setOnClickListener {
            showNotificationsPreviewDialog()
        }

        binding.rvButtonOne.setOnClickListener {
            showNotificationsButtonViewDialog(1)
        }

        binding.rvButtonTwo.setOnClickListener {
            showNotificationsButtonViewDialog(2)
        }

        binding.rvButtonThree.setOnClickListener {
            showNotificationsButtonViewDialog(3)
        }
    }

    private fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun showNotificationsPreviewDialog() {
        val dialog = Dialog(this)
        val dialogNotificationPreviewBinding: DialogNotificationsPreviewBinding =
            DialogNotificationsPreviewBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogNotificationPreviewBinding.root)

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

        when (notificationPreview) {
            resources.getString(R.string.show_name_message) -> {
                dialogNotificationPreviewBinding.rbNameAndMessage.isChecked = true
            }

            resources.getString(R.string.show_name) -> {
                dialogNotificationPreviewBinding.rbOnlyName.isChecked = true
            }

            resources.getString(R.string.hide_contents) -> {
                dialogNotificationPreviewBinding.rbHideContent.isChecked = true
            }
        }

        dialogNotificationPreviewBinding.rgNotificationSelection.setOnCheckedChangeListener { _, checkedId ->
            val selectedValue = dialog.findViewById<RadioButton>(checkedId)
            if (selectedValue != null) {
                notificationPreview = selectedValue.text.toString()
            }
        }

        dialogNotificationPreviewBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogNotificationPreviewBinding.btnApply.setOnClickListener {
            dialog.dismiss()
            SharedPreferencesHelper.saveString(
                this,
                Const.NOTIFICATIONS_PREVIEW,
                notificationPreview
            )
            binding.txtNotificationPreviewValue.text = notificationPreview
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    enum class NotificationAction(val resId: Int) {
        NONE(R.string.none),
        MARK_READ(R.string.mark_read),
        REPLY(R.string.reply),
        CALL(R.string.call),
        DELETE(R.string.delete)
    }

    private fun actionFromCheckedId(id: Int): String {
        return when (id) {
            R.id.rbNone -> getString(NotificationAction.NONE.resId)
            R.id.rbMarkRead -> getString(NotificationAction.MARK_READ.resId)
            R.id.rbReplay -> getString(NotificationAction.REPLY.resId)
            R.id.rbCall -> getString(NotificationAction.CALL.resId)
            R.id.rbDelete -> getString(NotificationAction.DELETE.resId)
            else -> getString(NotificationAction.NONE.resId)
        }
    }

    private fun checkRadio(
        binding: DialogNotificationButtonViewBinding,
        action: String
    ) {
        when (action) {
            getString(NotificationAction.NONE.resId) -> binding.rbNone.isChecked = true
            getString(NotificationAction.MARK_READ.resId) -> binding.rbMarkRead.isChecked = true
            getString(NotificationAction.REPLY.resId) -> binding.rbReplay.isChecked = true
            getString(NotificationAction.CALL.resId) -> binding.rbCall.isChecked = true
            getString(NotificationAction.DELETE.resId) -> binding.rbDelete.isChecked = true
        }
    }

    private fun showNotificationsButtonViewDialog(notificationButtonNumber: Number) {
        val dialog = Dialog(this)
        val dialogNotificationButtonViewBinding =
            DialogNotificationButtonViewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogNotificationButtonViewBinding.root)

        dialog.window?.apply {
            setBackgroundDrawableResource(R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0.6f)
            val metrics = resources.displayMetrics
            setLayout((metrics.widthPixels * 0.9f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        }

        val currentOne = SharedPreferencesHelper.getString(
            this, Const.NOTIFICATION_BUTTON_ONE, getString(NotificationAction.NONE.resId)
        )
        val currentTwo = SharedPreferencesHelper.getString(
            this, Const.NOTIFICATION_BUTTON_TWO, getString(NotificationAction.MARK_READ.resId)
        )
        val currentThree = SharedPreferencesHelper.getString(
            this, Const.NOTIFICATION_BUTTON_THREE, getString(NotificationAction.REPLY.resId)
        )

        var selectedAction = when (notificationButtonNumber.toInt()) {
            1 -> currentOne
            2 -> currentTwo
            else -> currentThree
        }

        checkRadio(dialogNotificationButtonViewBinding, selectedAction)

        dialogNotificationButtonViewBinding.rgNotificationAction.setOnCheckedChangeListener { _, checkedId ->
            selectedAction = actionFromCheckedId(checkedId)
        }

        dialogNotificationButtonViewBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialogNotificationButtonViewBinding.btnApply.setOnClickListener {
            when (notificationButtonNumber.toInt()) {
                1 -> {
                    if ((selectedAction != currentTwo && selectedAction != currentThree)
                        || selectedAction == getString(NotificationAction.NONE.resId)
                    ) {
                        SharedPreferencesHelper.saveString(
                            this,
                            Const.NOTIFICATION_BUTTON_ONE,
                            selectedAction
                        )
                        binding.txtButtonOne.text = getSelectedText(selectedAction)
                    } else {
                        showToast(getString(R.string.this_action_already_taken_into_others_button))
                    }
                }

                2 -> {
                    if ((selectedAction != currentOne && selectedAction != currentThree)
                        || selectedAction == getString(NotificationAction.NONE.resId)
                    ) {
                        SharedPreferencesHelper.saveString(
                            this,
                            Const.NOTIFICATION_BUTTON_TWO,
                            selectedAction
                        )
                        binding.txtButtonTwo.text = getSelectedText(selectedAction)
                    } else {
                        showToast(getString(R.string.this_action_already_taken_into_others_button))
                    }
                }

                3 -> {
                    if ((selectedAction != currentOne && selectedAction != currentTwo)
                        || selectedAction == getString(NotificationAction.NONE.resId)
                    ) {
                        SharedPreferencesHelper.saveString(
                            this,
                            Const.NOTIFICATION_BUTTON_THREE,
                            selectedAction
                        )
                        binding.txtButtonThree.text = getSelectedText(selectedAction)
                    } else {
                        showToast(getString(R.string.this_action_already_taken_into_others_button))
                    }
                }
            }
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun getSelectedText(action: String): String {
        return when (action) {
            getString(NotificationAction.NONE.resId) -> getString(R.string.none)
            getString(NotificationAction.MARK_READ.resId) -> getString(R.string.mark_read)
            getString(NotificationAction.REPLY.resId) -> getString(R.string.reply)
            getString(NotificationAction.CALL.resId) -> getString(R.string.call)
            getString(NotificationAction.DELETE.resId) -> getString(R.string.delete)
            else -> getString(R.string.none)
        }
    }
}

