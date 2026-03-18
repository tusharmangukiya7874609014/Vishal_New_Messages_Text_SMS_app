package com.texting.sms.messaging_app.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.CommonAdsLoadingDialogBinding
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

object InterstitialAdHelper {
    private var interstitialAd: InterstitialAd? = null
    private lateinit var loadingDialog: Dialog
    private var isAdLoading = false

    /** Load Interstitial Ad */
    fun loadAd(context: Context) {
        loadingDialog = Dialog(context)

        if (isAdLoading || interstitialAd != null) {
            return
        }

        isAdLoading = true

        InterstitialAd.load(
            AdRequest.Builder(
                SharedPreferencesHelper.getString(
                    context,
                    Const.INTERSTITIAL_ID,
                    Const.STRING_DEFAULT_VALUE
                )
            ).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingDialog.dismiss()
                    interstitialAd = ad
                    isAdLoading = false
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    loadingDialog.dismiss()
                }
            },
        )
    }

    /** Show Interstitial Ad */
    fun showAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) {
            onAdDismissed()
            return
        }

        if (interstitialAd == null) {
            loadAd(activity)
            onAdDismissed()
            return
        }

        loadingDialog = Dialog(activity)

        val delayMillis = SharedPreferencesHelper.getString(
            activity,
            Const.IS_LOADING_DIALOG_DELAY,
            Const.STRING_DEFAULT_VALUE
        )

        if (interstitialAd != null) {
            val adsCounter = SharedPreferencesHelper.getInt(
                context = activity,
                Const.IS_INTERSTITIAL_ENABLED_COUNT,
                Const.DEFAULT_VALUE_INT
            )

            val isAdsLoadingDialog = SharedPreferencesHelper.getBoolean(
                context = activity,
                Const.IS_ADS_LOADING_DIALOG,
                false
            )

            val isAdsEnabled = SharedPreferencesHelper.shouldShowAd(context = activity, adsCounter)

            if (isAdsEnabled) {
                if (isAdsLoadingDialog) {
                    showLoadingDialog(activity)

                    Handler(Looper.getMainLooper()).postDelayed({
                        interstitialAd?.adEventCallback =
                            object : InterstitialAdEventCallback {
                                override fun onAdShowedFullScreenContent() {
                                    Log.d("InterstitialAd", "Interstitial ad showed.")
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    loadingDialog.dismiss()
                                    interstitialAd = null
                                    onAdDismissed()
                                }

                                override fun onAdFailedToShowFullScreenContent(
                                    fullScreenContentError: FullScreenContentError
                                ) {
                                    loadingDialog.dismiss()
                                    interstitialAd = null
                                    onAdDismissed()
                                }

                                override fun onAdImpression() {}

                                override fun onAdClicked() {}
                            }

                        interstitialAd?.show(activity)

                    }, delayMillis.toLong())
                } else {
                    if (isAdLoading) {
                        showLoadingDialog(activity)
                    }

                    interstitialAd?.adEventCallback =
                        object : InterstitialAdEventCallback {
                            override fun onAdShowedFullScreenContent() {
                                Log.d("InterstitialAd", "Interstitial ad showed.")
                            }

                            override fun onAdDismissedFullScreenContent() {
                                loadingDialog.dismiss()
                                interstitialAd = null
                                onAdDismissed()
                            }

                            override fun onAdFailedToShowFullScreenContent(
                                fullScreenContentError: FullScreenContentError
                            ) {
                                loadingDialog.dismiss()
                                interstitialAd = null
                                onAdDismissed()
                            }

                            override fun onAdImpression() {}

                            override fun onAdClicked() {}
                        }

                    interstitialAd?.show(activity)
                }
            } else {
                loadingDialog.dismiss()
                onAdDismissed()
            }
        } else {
            loadingDialog.dismiss()
            onAdDismissed()
        }
    }

    /** Show a Loading Dialog */
    private fun showLoadingDialog(context: Context) {
        loadingDialog = Dialog(context)
        val layoutInflater = LayoutInflater.from(context)
        val customLoadingProgress = CommonAdsLoadingDialogBinding.inflate(layoutInflater)
        loadingDialog.setContentView(customLoadingProgress.root)
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        loadingDialog.show()
    }
}
