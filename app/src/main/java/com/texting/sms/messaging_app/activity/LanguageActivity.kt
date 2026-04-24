package com.texting.sms.messaging_app.activity

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.LanguageAdapter
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityLanguageBinding
import com.texting.sms.messaging_app.listener.LanguageInterface
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.model.AppLanguage
import com.texting.sms.messaging_app.services.CallOverlayService
import com.texting.sms.messaging_app.utils.LocaleHelper.setLocale
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LanguageActivity : BaseActivity(), LanguageInterface, NetworkAvailableListener {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var rvLanguageAdapter: LanguageAdapter
    private lateinit var languageList: List<AppLanguage>
    private var locale = "en"
    private var countryName = "English"

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_language)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebaseLogEvent(
            this@LanguageActivity, "LANGUAGE_SELECTED_PAGE", "LANGUAGE_SELECTED_PAGE"
        )
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@LanguageActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@LanguageActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@LanguageActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@LanguageActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@LanguageActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("LanguageActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@LanguageActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@LanguageActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@LanguageActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val bannerAdsType = getAdsPageResponse.optString("bannerAdsType").ifEmpty {
                "adaptive"
            } ?: "adaptive"

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isAppInterstitialAdsEnabled) {
                    InterstitialAdHelper.apply {
                        loadAd(this@LanguageActivity)
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

    private fun initView() {
        listOfLanguage()
    }

    private fun listOfLanguage() {
        languageList = listOf(
            AppLanguage("English", "(English)", "en"),
            AppLanguage("Hindi", "(हिंदी)", "hi"),
            AppLanguage("Spanish", "(español)", "es"),
            AppLanguage("Dutch", "(Nederlands)", "nl"),
            AppLanguage("German", "(Deutsch)", "de"),
            AppLanguage("Chinese", "(中国人)", "zh"),
            AppLanguage("Arabic", "(العربية)", "ar"),
            AppLanguage("Turkish", "(Türkçe)", "tr"),
            AppLanguage("Korean", "(한국인)", "ko"),
            AppLanguage("Tamil", "(தமிழ்)", "ta"),
            AppLanguage("Telugu", "(తెలుగు)", "te"),
            AppLanguage("Russian", "(Русский)", "ru"),
            AppLanguage("Estonian", "(eesti)", "et"),
            AppLanguage("Filipino", "(Filipino)", "tl"),
            AppLanguage("French", "(Français)", "fr"),
            AppLanguage("Danish", "(Dansk)", "da"),
            AppLanguage("Finnish", "(suomi)", "fi"),
            AppLanguage("Swedish", "(svensk)", "sv"),
            AppLanguage("Italian", "(Italiana)", "it"),
            AppLanguage("Norwegian", "(norsk)", "no"),
            AppLanguage("Vietnamese", "(Tiếng Việt)", "vi"),
            AppLanguage("Portuguese", "(Português)", "pt"),
            AppLanguage("Japanese", "(日本語)", "ja"),
            AppLanguage("Indonesian", "(Indonesia)", "id"),
            AppLanguage("Swahili", "(kiswahili)", "sw"),
        )

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvLanguageList.setLayoutManager(layoutManager)
        rvLanguageAdapter = LanguageAdapter(
            languageList = mutableListOf(), languageInterface = this, context = this
        )
        binding.rvLanguageList.adapter = rvLanguageAdapter

        if (languageList.isNotEmpty()) {
            rvLanguageAdapter.updateData(languageList)
        }
    }

    private fun initClickListener() {
        onBackPressedDispatcher.addCallback(
            this@LanguageActivity, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    startActivity(Intent(this@LanguageActivity, HomeActivity::class.java))
                    isEnabled = false
                    finish()
                }
            })

        binding.btnContinue.setOnClickListener {
            SharedPreferencesHelper.saveBoolean(this, "IS_FIRST_TIME_LANGUAGE_SELECTED", true)

            setLocale(this, locale)
            SharedPreferencesHelper.setLanguage(this, locale)
            SharedPreferencesHelper.saveString(
                this, Const.SELECTED_LANGUAGE_NAME, countryName
            )
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    override fun onItemClick(language: AppLanguage, position: Int) {
        countryName = language.countryName.toString()
        locale = language.languageCode.toString()
        rvLanguageAdapter.updateSelectedLanguage(position)
    }

    override fun onResume() {
        super.onResume()
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
    }

    private fun isFullScreenNotificationAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else true
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