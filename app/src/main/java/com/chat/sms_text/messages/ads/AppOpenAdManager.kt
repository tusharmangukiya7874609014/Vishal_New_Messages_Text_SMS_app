package com.chat.sms_text.messages.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

class AppOpenAdManager {
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
                    Log.d("AppOpenAd", "App open ad loaded.")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingAd = false
                    Log.d("AppOpenAd", "App open ad failed to load: $adError")
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

        appOpenAd?.adEventCallback =
            object : AppOpenAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    Log.d("AppOpenAd", "App open ad showed.")
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
                    isShowingAd = false
                    loadAd(activity)
                    onComplete()
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

    fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }
}