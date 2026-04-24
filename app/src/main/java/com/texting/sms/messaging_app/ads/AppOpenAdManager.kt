package com.texting.sms.messaging_app.ads

import android.app.Activity
import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper

class AppOpenAdManager {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadCoolDownTimer = 10000L
    var isShowingAd = false
    private var countDownTimer: CountDownTimer? = null
    private var isCooldownActive = false

    fun loadAd(context: Context) {
        if (isLoadingAd || isAdAvailable()) {
            Log.d("AppOpenAdManager", "App open ad is either loading or has already loaded.")
            return
        }

        if (isCooldownActive) {
            Log.d("AppOpenAdManager", "⛔ Cooldown active, skipping load")
            return
        }

        loadCoolDownTimer =
            SharedPreferencesHelper.getLong(context, Const.APP_OPEN_ADS_MINUTE_COUNTER, 10000)

        isLoadingAd = true
        isCooldownActive = true

        val appOpenAdsID = SharedPreferencesHelper.getString(
            context,
            Const.APP_OPEN_ADS_ID,
            Const.STRING_DEFAULT_VALUE
        )

        AppOpenAd.load(
            AdRequest.Builder(
                appOpenAdsID
            ).build(),
            object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    Log.d("AppOpenAdManager", "App open ad loaded.")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingAd = false
                    Log.d("AppOpenAdManager", "App open ad failed to load: $adError")
                }
            },
        )
    }

    fun showAdIfAvailable(activity: Activity, onComplete: () -> Unit) {
        if (isShowingAd) {
            Log.d("AppOpenAdManager", "App open ad is already showing.")
            return
        }

        if (!isAdAvailable()) {
            onComplete()
            Log.d("AppOpenAdManager", "App open ad is not ready yet.")
            loadAd(context = activity)
            return
        }

        appOpenAd?.adEventCallback =
            object : AppOpenAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    Log.d("AppOpenAdManager", "App open ad showed.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("AppOpenAdManager", "App open ad dismissed.")
                    startCooldownTimer(activity, loadCoolDownTimer)
                    appOpenAd = null
                    isShowingAd = false
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) {
                    Log.w("AppOpenAdManager", "App open ad failed to show: $fullScreenContentError")
                    appOpenAd = null
                    isShowingAd = false
                    loadAd(activity)
                    onComplete()
                }

                override fun onAdImpression() {
                    Log.d("AppOpenAdManager", "App open ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d("AppOpenAdManager", "App open ad recorded a click.")
                }
            }

        isShowingAd = true
        appOpenAd?.show(activity)
    }

    private fun startCooldownTimer(context: Context, durationMillis: Long) {
        countDownTimer?.cancel()

        Handler(Looper.getMainLooper()).post {
            countDownTimer = object : CountDownTimer(durationMillis, 1000) {

                override fun onTick(millisUntilFinished: Long) {
                    Log.d("AppOpenAdManager", "⏳ ${millisUntilFinished / 1000}s left")
                }

                override fun onFinish() {
                    Log.d("AppOpenAdManager", "✅ Cooldown finished")

                    isCooldownActive = false

                    loadAd(context)
                }

            }.start()
        }
    }

    fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }
}