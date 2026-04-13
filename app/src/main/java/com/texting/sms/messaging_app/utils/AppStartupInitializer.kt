package com.texting.sms.messaging_app.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.startup.Initializer
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.perf.FirebasePerformance
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AppStartupInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        FirebaseApp.initializeApp(context)

        CoroutineScope(Dispatchers.IO).launch {
            NotificationStore.initDatabase()
        }

        getAdsManagerResponse(context)
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

    private fun getAdsManagerResponse(context: Context) {
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
                        Log.e("ABCD", "Firebase Error :- ${e.localizedMessage}")
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

        SharedPreferencesHelper.saveBoolean(
            context,
            Const.IS_ADS_CONFIG_READY,
            true
        )

        if (SharedPreferencesHelper.getBoolean(
                context, Const.IS_ADS_ENABLED, false
            )
        ) {
            initializeAdsSafely(context) { }
        }
    }

    private fun initializeAdsSafely(context: Context, onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(
                    context,
                    InitializationConfig.Builder("ca-app-pub-5550085346779978~8891033445").build()
                ) {
                    Handler(Looper.getMainLooper()).post {
                        onComplete?.invoke()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun snapshotToJson(snapshot: DataSnapshot): String {
        val map = snapshot.value as Map<*, *>
        return JSONObject(map).toString()
    }
}