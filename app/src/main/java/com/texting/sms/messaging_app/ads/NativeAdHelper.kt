package com.texting.sms.messaging_app.ads

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NativeAdHelper {
    private var currentNativeAd: NativeAd? = null
    private var isLoading = false

    fun loadNativeAd(
        activity: Activity,
        adContainer: FrameLayout,
        nativeType: String,
        isCallBack: Boolean = false,
        nativeAdsId: String
    ) {
        activity.let {
            var finalNativeAdsID = nativeAdsId

            if (finalNativeAdsID.isEmpty()) {
                finalNativeAdsID = SharedPreferencesHelper.getString(
                    it, Const.NATIVE_ID, Const.STRING_DEFAULT_VALUE
                )
            }

            if (finalNativeAdsID.isEmpty() || isLoading) {
                it.runOnUiThread {
                    adContainer.visibility = View.GONE
                }
                return
            }

            it.runOnUiThread {
                isLoading = true
                adContainer.removeAllViews()
            }

            val inflater = LayoutInflater.from(it.fixedFontContext())
            val adView = inflateLayout(inflater, adContainer, nativeType, isCallBack)

            adView.let { adsView ->
                val shimmer =
                    adsView.findViewById<ShimmerFrameLayout>(R.id.shimmer_native_view_container)
                val rvFillNativeAdsView = adsView.findViewById<RelativeLayout>(R.id.rvFillNativeAds)

                it.runOnUiThread {
                    shimmer.startShimmer()
                }

                // Destroy previous ad safely
                currentNativeAd?.destroy()
                currentNativeAd = null

                val adRequest = NativeAdRequest.Builder(
                    nativeAdsId,
                    listOf(NativeAd.NativeAdType.NATIVE)
                ).build()

                val callback = object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) {
                        Log.d("NativeAd", "Native ad loaded.")
                        isLoading = false
                        currentNativeAd = nativeAd

                        it.runOnUiThread {
                            shimmer?.stopShimmer()
                            shimmer?.visibility = View.GONE
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            destroyNativeAd()
                            setEventCallback(nativeAd)
                            displayNativeAd(nativeAd, adsView)
                            currentNativeAd = nativeAd
                            rvFillNativeAdsView.visibility = View.VISIBLE
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("NativeAd", "Native ad failed to load: $adError")

                        it.runOnUiThread {
                            isLoading = false
                            adContainer.visibility = View.GONE
                        }
                    }
                }

                NativeAdLoader.load(adRequest, callback)
            }
        }
    }

    private fun setEventCallback(nativeAd: NativeAd) {
        nativeAd.adEventCallback =
            object : NativeAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    Log.d("NativeAd", "Native ad showed full screen content.")
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d("NativeAd", "Native ad dismissed full screen content.")
                }

                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) {
                    Log.d(
                        "NativeAd",
                        "Native ad failed to show full screen content with error: $fullScreenContentError",
                    )
                }

                override fun onAdImpression() {
                    Log.d("NativeAd", "Native ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d("NativeAd", "Native ad recorded a click.")
                }
            }
    }

    private fun displayNativeAd(nativeAd: NativeAd, adsView: View) {
        val nativeAdView = adsView.findViewById<NativeAdView>(R.id.native_ad_view)

        val headLineView = adsView.findViewById<TextView>(R.id.primary)
        val bodyView = adsView.findViewById<TextView>(R.id.body)
        val adsActionButton = adsView.findViewById<AppCompatButton>(R.id.cta)
        val adsIcon = adsView.findViewById<AppCompatImageView>(R.id.icon)
        val adAdvertiserView = adsView.findViewById<TextView>(R.id.ad_notification_view)
        val mediaView = adsView.findViewById<MediaView>(R.id.media_view)

        nativeAdView.advertiserView = adAdvertiserView
        nativeAdView.bodyView = bodyView
        nativeAdView.callToActionView = adsActionButton
        nativeAdView.headlineView = headLineView
        nativeAdView.iconView = adsIcon

        headLineView.text = nativeAd.headline
        bodyView.text = nativeAd.body
        adsActionButton.text = nativeAd.callToAction
        adsIcon.setImageDrawable(nativeAd.icon?.drawable)

        nativeAdView.registerNativeAd(nativeAd, mediaView)
    }

    private fun inflateLayout(
        inflater: LayoutInflater,
        container: FrameLayout,
        type: String,
        isCallBack: Boolean
    ): View {
        val layoutRes = when (type) {
            "customize_small" ->
                R.layout.layout_ads_customize_small

            "customize_banner" ->
                R.layout.layout_customize_ads_native_banner

            "default_small" ->
                R.layout.layout_native_small_template

            "default_medium" ->
                R.layout.layout_native_medium_template

            "customize_medium" ->
                if (isCallBack)
                    R.layout.layout_customize_medium_banner_dark
                else
                    R.layout.layout_customize_medium_banner

            else ->
                R.layout.layout_customize_ads_native_banner
        }

        return inflater.inflate(layoutRes, container, true)
    }

    fun Context.fixedFontContext(): Context {
        val config = Configuration(resources.configuration)
        config.fontScale = 1.05f
        val fixedContext = createConfigurationContext(config)
        return ContextThemeWrapper(fixedContext, R.style.Theme_CustomTheme)
    }

    private fun destroyNativeAd() {
        currentNativeAd?.destroy()
        currentNativeAd = null
        isLoading = false
    }
}