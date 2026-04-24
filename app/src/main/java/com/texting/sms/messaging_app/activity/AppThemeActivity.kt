package com.texting.sms.messaging_app.activity

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ActivityAppThemeBinding
import com.texting.sms.messaging_app.ads.NativeAdHelper
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.model.AppTheme
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppThemeActivity : BaseActivity(), NetworkAvailableListener {
    private lateinit var binding: ActivityAppThemeBinding
    private lateinit var originalTheme: AppTheme
    private var clickCount = 0

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_app_theme)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        clickCount = SharedPreferencesHelper.getInt(this, "COUNT_CLICK", 0)
        initView()
        initClickListener()
    }

    private fun runAdsCampion() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(300)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@AppThemeActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) return@launch

            val isAppNativeAdsEnabled = SharedPreferencesHelper.getBoolean(
                this@AppThemeActivity, Const.IS_NATIVE_ENABLED, false
            )

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@AppThemeActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("AppThemeActivity")
                    ?: return@launch

            val isCurrentPageAdsEnabled = getAdsPageResponse.optBoolean("isAdsShowing")

            if (!isCurrentPageAdsEnabled) return@launch

            val isCurrentPageNativeAdsEnabled =
                getAdsPageResponse.optBoolean("isNativeAdsShowing") && isAppNativeAdsEnabled

            val nativeAdsType = getAdsPageResponse.optString("isNativeAdsType").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AppThemeActivity,
                    Const.IS_NATIVE_ADS_TYPE_DEFAULT,
                    Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            val currentPageNativeAdsId = getAdsPageResponse.optString("nativeAdsID").ifEmpty {
                SharedPreferencesHelper.getString(
                    this@AppThemeActivity, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            } ?: ""

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                if (isCurrentPageNativeAdsEnabled) {
                    if (binding.nativeAdContainer.isVisible) return@withContext

                    runNativeAds(
                        nativeAdsType = nativeAdsType, nativeAdsId = currentPageNativeAdsId
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

    private fun initView() {
        originalTheme = SharedPreferencesHelper.getSavedTheme(this)

        when (originalTheme) {
            AppTheme.LIGHT -> {
                binding.ivSelectedMarkLight.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.img_check
                    )
                )
                binding.ivSelectedMarkDark.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.bg_oval_selected
                    )
                )
            }

            AppTheme.DARK -> {
                binding.ivSelectedMarkDark.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.img_check
                    )
                )
                binding.ivSelectedMarkLight.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.bg_oval_selected
                    )
                )
            }
        }
    }

    private fun initClickListener() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val theme = SharedPreferencesHelper.getSavedTheme(this@AppThemeActivity)
                when (theme) {
                    AppTheme.LIGHT -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }

                    AppTheme.DARK -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                }

                if (clickCount != 0) {
                    val intent = Intent(this@AppThemeActivity, HomeActivity::class.java)
                    val options = ActivityOptions.makeCustomAnimation(
                        this@AppThemeActivity,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    startActivity(intent, options.toBundle())
                    finishAffinity()
                } else {
                    isEnabled = false
                    finish()
                }
            }
        })

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvLightTheme.setOnClickListener {
            if (originalTheme == AppTheme.LIGHT) {
                return@setOnClickListener
            }

            clickCount++
            SharedPreferencesHelper.saveInt(this, "COUNT_CLICK", clickCount)
            binding.ivSelectedMarkLight.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.img_check
                )
            )
            binding.ivSelectedMarkDark.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_oval_selected
                )
            )
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
            SharedPreferencesHelper.saveTheme(this, AppTheme.LIGHT)
        }

        binding.rvDarkTheme.setOnClickListener {
            if (originalTheme == AppTheme.DARK) {
                return@setOnClickListener
            }

            clickCount++
            SharedPreferencesHelper.saveInt(this, "COUNT_CLICK", clickCount)
            binding.ivSelectedMarkDark.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.img_check
                )
            )
            binding.ivSelectedMarkLight.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.bg_oval_selected
                )
            )
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            SharedPreferencesHelper.saveTheme(this, AppTheme.DARK)
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