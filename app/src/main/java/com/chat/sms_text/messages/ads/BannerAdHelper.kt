package com.chat.sms_text.messages.ads

import android.app.Activity
import android.content.res.Resources
import android.util.Log
import android.view.View
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

object BannerAdHelper {

    fun loadBannerAd(
        activity: Activity,
        adContainer: AdView,
        shimmerLayout: ShimmerFrameLayout,
        bannerAdsId: String = SharedPreferencesHelper.getString(
            activity, Const.BANNER_ID, Const.STRING_DEFAULT_VALUE
        ),
        view: View,
        bannerType: BannerType = BannerType.ADAPTIVE,
    ) {
        activity.let {
            val outMetrics = Resources.getSystem().displayMetrics
            val widthPixels = outMetrics.widthPixels.toFloat()
            val density = outMetrics.density
            val adWidth = (widthPixels / density).toInt()

            val adSize = when (bannerType) {
                BannerType.ADAPTIVE -> {
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                        it,
                        adWidth
                    )
                }

                BannerType.MEDIUM_RECTANGLE -> {
                    AdSize(adWidth, 250)
                }
            }

            it.runOnUiThread {
                shimmerLayout.startShimmer()
                shimmerLayout.visibility = View.VISIBLE
            }

            adContainer.loadAd(
                BannerAdRequest.Builder(bannerAdsId, adSize).build(),
                object : AdLoadCallback<BannerAd> {
                    override fun onAdLoaded(ad: BannerAd) {
                        ad.adEventCallback =
                            object : BannerAdEventCallback {
                                override fun onAdImpression() {
                                    Log.d("BannerAd", "Banner ad recorded an impression.")
                                }

                                override fun onAdClicked() {
                                    Log.d("BannerAd", "Banner ad recorded a click.")
                                }
                            }
                        ad.bannerAdRefreshCallback =
                            object : BannerAdRefreshCallback {
                                override fun onAdRefreshed() {
                                    Log.d("BannerAd", "Banner ad refreshed.")
                                }

                                override fun onAdFailedToRefresh(adError: LoadAdError) {
                                    Log.d("BannerAd", "Banner ad failed to refresh: $adError")
                                }
                            }

                        Log.d("BannerAd", "Banner ad loaded.")

                        it.runOnUiThread {
                            shimmerLayout.stopShimmer()
                            shimmerLayout.visibility = View.GONE
                            adContainer.visibility = View.VISIBLE
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.w("BannerAd", "Banner ad failed to load: $adError")
                        it.runOnUiThread {
                            shimmerLayout.stopShimmer()
                            shimmerLayout.visibility = View.GONE
                            adContainer.visibility = View.GONE
                            view.visibility = View.GONE
                        }
                    }
                },
            )
        }
    }
}