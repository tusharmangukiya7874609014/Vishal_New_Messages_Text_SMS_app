package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.ads.AdsManager
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityCustomLunchBinding
import com.texting.sms.messaging_app.listener.NetworkAvailableListener
import com.texting.sms.messaging_app.utils.NetworkConnectionUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CustomLunchActivity : BaseActivity(), NetworkAvailableListener {
    private lateinit var binding: ActivityCustomLunchBinding

    private lateinit var consentInformation: ConsentInformation
    private lateinit var appOpenAdManager: AppOpenAdManager
    private var splashTimeoutRunnable: Runnable? = null
    private var isNavigationDone = false

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isFlowStarted = false

    private lateinit var networkUtil: NetworkConnectionUtil
    private var isNetworkConnection = false

    private var isStartProcess = false

    companion object {
        private const val MAX_ADS_WAIT = 5000L
        private const val UI_DELAY = 1000L
        private const val UI_DELAY_NO_INTERNET_CONNECTION = 1000L
    }

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

        binding = DataBindingUtil.setContentView(this, R.layout.activity_custom_lunch)
        SharedPreferencesHelper.saveBoolean(
            this@CustomLunchActivity, Const.IS_ADS_CONFIG_READY, false
        )
    }

    private fun initConsentInformation() {
        val params = ConsentRequestParameters.Builder().build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation.requestConsentInfoUpdate(this@CustomLunchActivity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this@CustomLunchActivity) { loadAndShowError ->
                if (loadAndShowError != null) {
                    Log.e(
                        "ConsentInformation",
                        "Consent Load And Show Error :- ${loadAndShowError.message}"
                    )
                    return@loadAndShowConsentFormIfRequired
                }

                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsSdk()
                }
            }
        }, { error ->
            Log.e("ConsentInformation", "Consent Error :- ${error.message}")
        })
    }

    private fun initializeMobileAdsSdk() {
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            val sharePreference = getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)

            val purposeConsents = sharePreference.getString("IABTCF_PurposeConsents", "")

            if (!purposeConsents.isNullOrEmpty()) {
                val purposeOneString = purposeConsents.first().toString()
                val hasConsentForPurposeOne = purposeOneString == "1"

                val isAdsEnabled = SharedPreferencesHelper.getBoolean(
                    this@CustomLunchActivity, Const.IS_ADS_ENABLED, false
                )

                if (hasConsentForPurposeOne && isAdsEnabled) {
                    firebaseLogEvent(this@CustomLunchActivity, "SPLASH_PAGE", "CONSENT_ALLOWED")

                    AdsManager.initialize(this@CustomLunchActivity) {
                        if (isFlowStarted) return@initialize
                        isFlowStarted = true

                        mainHandler.post {
                            startSplashFlow()
                        }
                    }
                } else {
                    executeNextTask()
                }
            } else {
                val isAdsEnabled = SharedPreferencesHelper.getBoolean(
                    this@CustomLunchActivity, Const.IS_ADS_ENABLED, false
                )

                if (isAdsEnabled) {
                    AdsManager.initialize(this@CustomLunchActivity) {
                        if (isFlowStarted) return@initialize
                        isFlowStarted = true

                        mainHandler.post {
                            startSplashFlow()
                        }
                    }
                } else {
                    executeNextTask()
                }
            }
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val readPhoneStateGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        return notificationGranted && readPhoneStateGranted
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun startSplashFlow() {
        waitForAdsConfigWithTimeout()
    }

    private fun waitForAdsConfigWithTimeout() {
        val startTime = System.currentTimeMillis()

        val checkRunnable = object : Runnable {
            override fun run() {
                val isReady = SharedPreferencesHelper.getBoolean(
                    this@CustomLunchActivity, Const.IS_ADS_CONFIG_READY, false
                )

                if (isReady) {
                    loadAppOpenAdIfNeeded()
                    SharedPreferencesHelper.saveBoolean(
                        this@CustomLunchActivity, Const.IS_ADS_CONFIG_READY, false
                    )
                } else if (System.currentTimeMillis() - startTime > MAX_ADS_WAIT) {
                    executeNextTask()
                    SharedPreferencesHelper.saveBoolean(
                        this@CustomLunchActivity, Const.IS_ADS_CONFIG_READY, false
                    )
                } else {
                    Log.d("ABCD","isStartProcess $isStartProcess")

                    if (!isStartProcess) getAdsManagerResponse(this@CustomLunchActivity)

                    mainHandler.postDelayed(this, 250)
                }
            }
        }

        mainHandler.post(checkRunnable)
    }

    private fun loadAppOpenAdIfNeeded() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(150)

            val isAppAdsShowing = SharedPreferencesHelper.getBoolean(
                this@CustomLunchActivity, Const.IS_ADS_ENABLED, false
            )

            if (!isAppAdsShowing) {
                withContext(Dispatchers.Main) {
                    executeNextTask()
                }
                return@launch
            }

            val retrievedAdsJson = SharedPreferencesHelper.getJsonFromPreferences(
                this@CustomLunchActivity, Const.MESSAGE_MANAGER_RESPONSE
            )

            val getAdsPageResponse =
                retrievedAdsJson.optJSONObject("activities")?.optJSONObject("CustomLunchActivity")
                    ?: return@launch

            val isAppOpenAdsEnabled = getAdsPageResponse.optBoolean("isAppOpenAdsEnabled")

            if (!isAppOpenAdsEnabled) {
                withContext(Dispatchers.Main) {
                    executeNextTask()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext

                binding.txtPleaseWaitAds.visibility = View.VISIBLE

                startSplashTimeout()

                appOpenAdManager = AppOpenAdManager().apply {
                    loadAd(this@CustomLunchActivity)
                }
            }
        }
    }

    private fun startSplashTimeout() {
        splashTimeoutRunnable = Runnable {
            if (!isNavigationDone) {
                isNavigationDone = true
                executeNextTask()
            }
        }

        splashTimeoutRunnable?.let {
            mainHandler.postDelayed(it, 5000L)
        }
    }

    private fun cancelSplashTimeout() {
        splashTimeoutRunnable?.let {
            mainHandler.removeCallbacks(it)
        }
        splashTimeoutRunnable = null
    }

    override fun onNetworkAvailable() {
        runOnUiThread {
            isNetworkConnection = true

            binding.txtPleaseWaitAds.visibility = View.INVISIBLE

            initConsentInformation()
        }
    }

    override fun onNetworkLost() {
        runOnUiThread {
            isNetworkConnection = false

            val finalText =
                getString(R.string.you_re_offline_turn_on_your_internet_connection_to_access_all_messages_features)

            binding.txtPleaseWaitAds.text = finalText
            binding.txtPleaseWaitAds.visibility = View.VISIBLE

            executeNextTask()
        }
    }

    private fun executeNextTask() {
        val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
            this, "IS_FIRST_TIME_LANGUAGE_SELECTED", false
        )

        val nextIntent = when {
            !hasRequiredPermissions() -> {
                Intent(this, ReadPermissionActivity::class.java)
            }

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

        val delaySeconds = if (isNetworkConnection) UI_DELAY else UI_DELAY_NO_INTERNET_CONNECTION

        lifecycleScope.launch {
            delay(delaySeconds)
            startActivity(nextIntent)
            finish()
        }
    }

    private fun getAdsManagerResponse(context: Context) {
        isStartProcess = true

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("messageManager")

        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val jsonString = snapshotToJson(snapshot)
                        val jsonObject = JSONObject(jsonString)

                        SharedPreferencesHelper.saveJsonToPreferences(
                            context, Const.MESSAGE_MANAGER_RESPONSE, jsonObject
                        )

                        storeAdsManagerResponse(context = context)
                    } catch (e: Exception) {
                        Log.e("FirebaseError", "Firebase Error :- ${e.localizedMessage}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Firebase Error fetching data: ${error.message}")
            }
        })
    }

    private fun storeAdsManagerResponse(context: Context) {
        val retrievedAdsJson =
            SharedPreferencesHelper.getJsonFromPreferences(context, Const.MESSAGE_MANAGER_RESPONSE)

        SharedPreferencesHelper.saveBoolean(
            context, Const.IS_ADS_ENABLED, retrievedAdsJson.optBoolean("isAdsShowingOrNot")
        )
        SharedPreferencesHelper.saveBoolean(
            context, Const.IS_BANNER_ENABLED, retrievedAdsJson.optBoolean("isBannerAdsLoads")
        )
        SharedPreferencesHelper.saveBoolean(
            context, Const.IS_NATIVE_ENABLED, retrievedAdsJson.optBoolean("isNativeAdsLoad")
        )
        SharedPreferencesHelper.saveBoolean(
            context,
            Const.IS_INTERSTITIAL_ENABLED,
            retrievedAdsJson.optBoolean("isInterstitialAdsLoad")
        )
        SharedPreferencesHelper.saveBoolean(
            context,
            Const.IS_APP_OPEN_ENABLED,
            retrievedAdsJson.optBoolean("isAppOpenAdsShowingOrNot")
        )
        SharedPreferencesHelper.saveBoolean(
            context, Const.IS_ADS_LOADING_DIALOG, retrievedAdsJson.optBoolean("isAdsLoadingDialog")
        )
        SharedPreferencesHelper.saveBoolean(
            context,
            Const.IS_TRANSLATE_VISIBLE_OR_NOT,
            retrievedAdsJson.optBoolean("isTranslateVisibleOrNot")
        )

        SharedPreferencesHelper.saveString(
            context, Const.BANNER_ID, retrievedAdsJson.optString("bannerAdsID")
        )
        SharedPreferencesHelper.saveString(
            context, Const.INTERSTITIAL_ID, retrievedAdsJson.optString("interstitialAdsID")
        )
        SharedPreferencesHelper.saveString(
            context, Const.NATIVE_ID, retrievedAdsJson.optString("nativeAdsID")
        )
        SharedPreferencesHelper.saveString(
            context, Const.APP_OPEN_ADS_ID, retrievedAdsJson.optString("appOpenAdsID")
        )
        SharedPreferencesHelper.saveString(
            context,
            Const.IS_LOADING_DIALOG_DELAY,
            retrievedAdsJson.optString("isDialogLoadingDelay")
        )
        SharedPreferencesHelper.saveString(
            context,
            Const.IS_NATIVE_ADS_TYPE_DEFAULT,
            retrievedAdsJson.optString("isNativeAdsByDefault")
        )
        SharedPreferencesHelper.saveString(
            context, Const.PRIVACY_URL, retrievedAdsJson.optString("privacyPolicy")
        )

        SharedPreferencesHelper.saveInt(
            context,
            Const.IS_INTERSTITIAL_ENABLED_COUNT,
            retrievedAdsJson.optInt("isInterstitialAdsShowedCount")
        )

        SharedPreferencesHelper.saveLong(
            context,
            Const.INTERSTITIAL_ADS_MINUTE_COUNT,
            retrievedAdsJson.optLong("interstitialAdsMinuteCount")
        )

        SharedPreferencesHelper.saveLong(
            context,
            Const.APP_OPEN_ADS_MINUTE_COUNTER,
            retrievedAdsJson.optLong("appOpenAdsMinuteCount")
        )

        SharedPreferencesHelper.saveBoolean(
            context, Const.IS_ADS_CONFIG_READY, true
        )
    }

    private fun snapshotToJson(snapshot: DataSnapshot): String {
        val map = snapshot.value as Map<*, *>
        return JSONObject(map).toString()
    }

    private inner class AppOpenAdManager {
        private var appOpenAd: AppOpenAd? = null
        private var isLoadingAd = false
        var isShowingAd = false

        fun loadAd(context: Context) {
            if (isLoadingAd || isAdAvailable()) {
                Log.d("AppOpenAd", "App open ad is either loading or has already loaded.")
                return
            }

            isLoadingAd = true
            val appOpenAdsID = SharedPreferencesHelper.getString(
                context, Const.APP_OPEN_ADS_ID, Const.STRING_DEFAULT_VALUE
            )

            AppOpenAd.load(
                AdRequest.Builder(
                    appOpenAdsID
                ).build(),
                object : AdLoadCallback<AppOpenAd> {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        firebaseLogEvent(
                            this@CustomLunchActivity, "SPLASH_PAGE", "APP_OPEN_ADS_LOADED"
                        )
                        appOpenAd = ad
                        isLoadingAd = false
                        Log.d("AppOpenAd", "App open ad loaded.")

                        mainHandler.postDelayed({
                            if (!isNavigationDone) {
                                showAdIfAvailable(this@CustomLunchActivity) {
                                    appOpenAd = null
                                    if (!isNavigationDone) {
                                        isNavigationDone = true
                                        cancelSplashTimeout()
                                        executeNextTask()
                                    }
                                }
                            }
                        }, 150)
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("AppOpenAd", "App open ad failed to load: $adError")
                        isLoadingAd = false
                        if (!isNavigationDone) {
                            isNavigationDone = true
                            cancelSplashTimeout()
                            executeNextTask()
                        }
                    }
                },
            )
        }

        fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit) {
            if (isShowingAd) {
                Log.d("AppOpenAd", "App open ad is already showing.")
                return
            }

            if (!isAdAvailable()) {
                onComplete()
                Log.d("AppOpenAd", "App open ad is not ready yet.")
                loadAd(context = activity)
                return
            }

            appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    firebaseLogEvent(this@CustomLunchActivity, "SPLASH_PAGE", "APP_OPEN_ADS_SHOWED")
                    Log.d("AppOpenAd", "App open ad showed.")
                    cancelSplashTimeout()
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("AppOpenAd", "App open ad dismissed.")
                    appOpenAd = null
                    isShowingAd = false
                    loadAd(activity)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) {
                    Log.w("AppOpenAd", "App open ad failed to show: $fullScreenContentError")
                    appOpenAd = null
                    if (!isNavigationDone) {
                        isNavigationDone = true
                        cancelSplashTimeout()
                        executeNextTask()
                    }
                }

                override fun onAdImpression() {
                    Log.d("AppOpenAd", "App open ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d("AppOpenAd", "App open ad recorded a click.")
                }
            }

            isShowingAd = true
            appOpenAd?.show(activity)
        }

        private fun isAdAvailable(): Boolean {
            return appOpenAd != null
        }
    }
}