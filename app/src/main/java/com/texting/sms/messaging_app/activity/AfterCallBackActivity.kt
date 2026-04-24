package com.texting.sms.messaging_app.activity

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityAfterCallBackBinding
import com.texting.sms.messaging_app.fragment.CustomMessageFragment
import com.texting.sms.messaging_app.fragment.SendMessagesFragment
import com.texting.sms.messaging_app.fragment.SettingsFragment
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import com.texting.sms.messaging_app.viewmodel.AfterCallBackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AfterCallBackActivity : AppCompatActivity(), NetworkAvailableListener {
    private lateinit var binding: ActivityAfterCallBackBinding
    private val viewModel: AfterCallBackViewModel by viewModels()

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
        enableEdgeToEdge()
        networkUtil = NetworkConnectionUtil(this)
        networkUtil.setListener(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_after_call_back)
        firebaseLogEvent(this)
        initView()
        initObserver()
        initClickListener()
    }

    private fun firebaseLogEvent(
        context: Context
    ) {
        val bundle = Bundle().apply {
            putString("page_name", "AFTER_CALL_BACK_PAGE_SHOWN")
        }

        FirebaseAnalytics.getInstance(context).logEvent("SHOW_AFTER_CALL_BACK_PAGE", bundle)
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@AfterCallBackActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AfterCallBackActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AfterCallBackActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@AfterCallBackActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("AfterCallBackActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AfterCallBackActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AfterCallBackActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AfterCallBackActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        bannerAdsId = currentPageBannerAdsID,
                        bannerAdsType = bannerAdsType
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
        if (intent.hasExtra("ClickView")) {
            val duration = intent.getStringExtra("Duration")
            binding.txtCallDuration.text = duration

            val openSpecificView = intent.getIntExtra("ClickView", 1)
            viewModel.selectTab(openSpecificView)

            val callType = intent.getStringExtra("CallType")
            binding.txtCalledType.text = callType
        } else {
            viewModel.selectTab(1)
        }
    }

    private fun initObserver() {
        viewModel.selectedTab.observe(this) { tab ->
            setAnimationTab(tab)
            openFragment(tab)
        }
    }

    private fun setAnimationTab(tab: Int) {
        binding.selectedView = tab

        scaleView(binding.ivNotes, if (tab == 1) 1.1f else 1f)
        scaleView(binding.ivMessage, if (tab == 2) 1.2f else 1f)
        scaleView(binding.ivOtherSettings, if (tab == 3) 1.1f else 1f)
    }

    private fun scaleView(view: View, scale: Float) {
        view.animate().cancel()
        view.animate().scaleX(scale).scaleY(scale).setDuration(250)
            .setInterpolator(FastOutSlowInInterpolator()).start()
    }

    private fun openFragment(tab: Int) {
        when (tab) {
            1 -> replaceFragment(this, SendMessagesFragment(), "View")
            2 -> replaceFragment(this, CustomMessageFragment(), "Messages")
            3 -> replaceFragment(this, SettingsFragment(), "Settings")
        }
    }

    private fun initClickListener() {
        binding.rvViewMessages.setOnClickListener {
            viewModel.selectTab(1)
        }

        binding.rvMessage.setOnClickListener {
            viewModel.selectTab(2)
        }

        binding.rvOtherSettings.setOnClickListener {
            viewModel.selectTab(3)
        }
    }

    private fun replaceFragment(
        activity: FragmentActivity, fragmentToShow: Fragment, tag: String
    ) {
        val fragmentManager = activity.supportFragmentManager
        if (fragmentManager.isStateSaved) return
        val transaction = fragmentManager.beginTransaction()

        val currentFragment = fragmentManager.fragments.firstOrNull { it.isVisible }

        if (currentFragment?.tag == tag) return

        currentFragment?.let {
            transaction.hide(it)
        }

        val existingFragment = fragmentManager.findFragmentByTag(tag)

        if (existingFragment == null) {
            transaction.add(R.id.fragmentView, fragmentToShow, tag)
        } else {
            transaction.show(existingFragment)
        }

        transaction.setReorderingAllowed(true)
        transaction.commit()
    }

    override fun onResume() {
        enableImmersiveModeWithTransparentStatusBar()
        super.onResume()
    }

    private fun enableImmersiveModeWithTransparentStatusBar() {
        val window = window
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.apply {
                setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
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