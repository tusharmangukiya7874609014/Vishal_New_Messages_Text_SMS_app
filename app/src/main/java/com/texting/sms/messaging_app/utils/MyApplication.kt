package com.texting.sms.messaging_app.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.perf.FirebasePerformance
import com.texting.sms.messaging_app.activity.AdsAppOpenActivity
import com.texting.sms.messaging_app.ads.AdsManager
import com.texting.sms.messaging_app.ads.AppOpenAdManager
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.model.AppTheme
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application(), Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    lateinit var appOpenAdManager: AppOpenAdManager
    private var currentActivity: Activity? = null
    private var isAppInBackground = true

    override fun onCreate() {
        super<Application>.onCreate()
        FirebaseApp.initializeApp(this)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        EmojiManager.install(GoogleEmojiProvider())
        NotificationStore.initDatabase()

        applyTheme(SharedPreferencesHelper.getSavedTheme(this))
        SharedPreferencesHelper.saveInt(this, Const.IS_INTERSTITIAL_COUNT, 0)
        SharedPreferencesHelper.saveBoolean(this, Const.IS_FIRST_TIME_APP_OPEN, true)

        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        appOpenAdManager = AppOpenAdManager()

        val sharePreference = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)

        val purposeConsents = sharePreference.getString("IABTCF_PurposeConsents", "")
        if (!purposeConsents.isNullOrEmpty()) {
            val purposeOneString = purposeConsents.first().toString()
            val hasConsentForPurposeOne = purposeOneString == "1"

            val isAdsEnabled = SharedPreferencesHelper.getBoolean(
                this, Const.IS_ADS_ENABLED, false
            )

            val isAppOpenAdsEnabled = SharedPreferencesHelper.getBoolean(
                this, Const.IS_APP_OPEN_ENABLED, false
            )

            if (hasConsentForPurposeOne && isAdsEnabled && isAppOpenAdsEnabled) {
                if (!AdsManager.isReady()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            MobileAds.initialize(
                                applicationContext,
                                InitializationConfig.Builder("ca-app-pub-5550085346779978~8891033445")
                                    .build()
                            ) {
                                Handler(Looper.getMainLooper()).post {
                                    appOpenAdManager.loadAd(applicationContext)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    appOpenAdManager.loadAd(applicationContext)
                }
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(adjustFontScale(base))
    }

    private fun applyTheme(theme: AppTheme) {
        when (theme) {
            AppTheme.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppTheme.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun adjustFontScale(context: Context): Context {
        val configuration = context.resources.configuration
        if (configuration.fontScale != 1.0f) {
            configuration.fontScale = 1.0f
            return context.createConfigurationContext(configuration)
        }
        return context
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        isAppInBackground = true
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val isNoAdsAppOpenAds = SharedPreferencesHelper.getBoolean(this, "NO_ADS_APP_OPEN", false)

        if (isNoAdsAppOpenAds) {
            SharedPreferencesHelper.saveBoolean(this, "NO_ADS_APP_OPEN", false)
            return
        }

        if (!isAppInBackground) return
        isAppInBackground = false

        val isAdsEnabled = SharedPreferencesHelper.getBoolean(
            this, Const.IS_ADS_ENABLED, false
        )

        if (!isAdsEnabled) return

        val isAppOpenAdsEnabled = SharedPreferencesHelper.getBoolean(
            this, Const.IS_APP_OPEN_ENABLED, false
        )

        if (!isAppOpenAdsEnabled) return

        if (!appOpenAdManager.isAdAvailable()) {
            val sharePreference = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)

            val purposeConsents = sharePreference.getString("IABTCF_PurposeConsents", "")
            if (!purposeConsents.isNullOrEmpty()) {
                val purposeOneString = purposeConsents.first().toString()
                val hasConsentForPurposeOne = purposeOneString == "1"

                if (hasConsentForPurposeOne ) {
                    if (!AdsManager.isReady()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                MobileAds.initialize(
                                    applicationContext,
                                    InitializationConfig.Builder("ca-app-pub-5550085346779978~8891033445")
                                        .build()
                                ) {
                                    Handler(Looper.getMainLooper()).post {
                                        appOpenAdManager.loadAd(applicationContext)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        appOpenAdManager.loadAd(applicationContext)
                    }
                }
            } else {
                if (!AdsManager.isReady()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            MobileAds.initialize(
                                applicationContext,
                                InitializationConfig.Builder("ca-app-pub-5550085346779978~8891033445")
                                    .build()
                            ) {
                                Handler(Looper.getMainLooper()).post {
                                    appOpenAdManager.loadAd(applicationContext)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    appOpenAdManager.loadAd(applicationContext)
                }
            }
            return
        }

        val blockedScreens = setOf(
            "CustomLunchActivity",
            "DefaultPermissionActivity",
            "PrivacyPolicyActivity",
            "NotificationsActivity",
            "AfterCallBackActivity",
            "OverlayPermissionActivity",
            "PermissionOverlayActivity",
            "SetPasswordActivity",
            "ReadPermissionActivity",
            "VerifyPasswordActivity",
            "SecurityQuestionsActivity"
        )

        val currentName = currentActivity?.javaClass?.simpleName

        if (currentName !in blockedScreens) {
            currentActivity?.let {
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, AdsAppOpenActivity::class.java)
                    it.startActivity(intent)
                }, 300)
            }
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (::appOpenAdManager.isInitialized && !appOpenAdManager.isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, savedInstanceState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}