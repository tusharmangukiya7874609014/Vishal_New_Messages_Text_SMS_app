package com.texting.sms.messaging_app.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.BannerAdHelper
import com.texting.sms.messaging_app.ads.BannerType
import com.texting.sms.messaging_app.ads.InterstitialAdHelper
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityChangeLanguageBinding
import com.texting.sms.messaging_app.listener.LanguageInterface
import com.texting.sms.messaging_app.model.AppLanguage
import com.texting.sms.messaging_app.utils.LocaleHelper.setLocale
import com.texting.sms.messaging_app.adapter.LanguageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangeLanguageActivity : BaseActivity(), LanguageInterface {
    private lateinit var binding: ActivityChangeLanguageBinding
    private lateinit var rvLanguageAdapter: LanguageAdapter
    private lateinit var languageList: List<AppLanguage>
    private var countryName = "English"
    private var locale = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_change_language)
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
                this@ChangeLanguageActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppInterstitialAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChangeLanguageActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChangeLanguageActivity, Const.IS_NATIVE_ENABLED, false
            )

            val isAppBannerAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChangeLanguageActivity, Const.IS_BANNER_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@ChangeLanguageActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")
                    ?.optJSONObject("ChangeLanguageActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageBannerAdsEnabled =
                getAdsPageResponse.optBoolean("isBannerAdsShowing") && isAppBannerAdsEnabled

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ChangeLanguageActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ChangeLanguageActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageBannerAdsID = getAdsPageResponse.optString("bannerAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@ChangeLanguageActivity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
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
                        loadAd(this@ChangeLanguageActivity)
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
        listOfLanguage()
    }

    private fun listOfLanguage() {
        languageList = listOf(
            AppLanguage("English", "(English)", "en"),
            AppLanguage("Hindi", "(हिंदी)", "hi"),
            AppLanguage("Chinese", "(中国人)", "zh"),
            AppLanguage("Arabic", "(العربية)", "ar"),
            AppLanguage("Turkish", "(Türkçe)", "tr"),
            AppLanguage("Korean", "(한국인)", "ko"),
            AppLanguage("Tamil", "(தமிழ்)", "ta"),
            AppLanguage("Telugu", "(తెలుగు)", "te"),
            AppLanguage("Russian", "(Русский)", "ru"),
            AppLanguage("Dutch", "(Nederlands)", "nl"),
            AppLanguage("Estonian", "(eesti)", "et"),
            AppLanguage("Filipino", "(Filipino)", "tl"),
            AppLanguage("German", "(Deutsch)", "de"),
            AppLanguage("French", "(Français)", "fr"),
            AppLanguage("Danish", "(Dansk)", "da"),
            AppLanguage("Finnish", "(suomi)", "fi"),
            AppLanguage("Swedish", "(svensk)", "sv"),
            AppLanguage("Italian", "(Italiana)", "it"),
            AppLanguage("Spanish", "(español)", "es"),
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
            languageList = mutableListOf(),
            languageInterface = this,
            context = this
        )
        binding.rvLanguageList.adapter = rvLanguageAdapter

        if (languageList.isNotEmpty()) {
            rvLanguageAdapter.updateData(languageList)
        }
    }

    private fun initClickListener() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val adsEnabled = SharedPreferencesHelper.getBoolean(
                    this@ChangeLanguageActivity, Const.IS_ADS_ENABLED, false
                )
                val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                    this@ChangeLanguageActivity, Const.IS_INTERSTITIAL_ENABLED, false
                )

                if (adsEnabled && interstitialEnabled) {
                    InterstitialAdHelper.showAd(this@ChangeLanguageActivity) {
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

        binding.btnChangeLanguage.setOnClickListener {
            val adsEnabled = SharedPreferencesHelper.getBoolean(
                this@ChangeLanguageActivity, Const.IS_ADS_ENABLED, false
            )
            val interstitialEnabled = SharedPreferencesHelper.getBoolean(
                this@ChangeLanguageActivity, Const.IS_INTERSTITIAL_ENABLED, false
            )

            if (adsEnabled && interstitialEnabled) {
                InterstitialAdHelper.showAd(this@ChangeLanguageActivity) {
                    setLocale(this@ChangeLanguageActivity, locale)
                    SharedPreferencesHelper.setLanguage(this@ChangeLanguageActivity, locale)
                    SharedPreferencesHelper.saveString(
                        this,
                        Const.SELECTED_LANGUAGE_NAME,
                        countryName
                    )
                    startActivity(Intent(this@ChangeLanguageActivity, HomeActivity::class.java))
                }
            } else {
                setLocale(this, locale)
                SharedPreferencesHelper.setLanguage(this, locale)
                SharedPreferencesHelper.saveString(
                    this,
                    Const.SELECTED_LANGUAGE_NAME,
                    countryName
                )
                startActivity(Intent(this, HomeActivity::class.java))
            }
            finishAffinity()
        }
    }

    override fun onItemClick(language: AppLanguage, position: Int) {
        countryName = language.countryName.toString()
        locale = language.languageCode.toString()
        rvLanguageAdapter.updateSelectedLanguage(position)
    }
}