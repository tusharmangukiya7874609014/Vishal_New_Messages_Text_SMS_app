package com.texting.sms.messaging_app.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AdsManager {
    private var isInitialized = false
    private var isInitializing = false

    private val pendingTasks = mutableListOf<() -> Unit>()

    fun initialize(activity: Activity, onComplete: (() -> Unit)? = null) {
        if (isInitialized) {
            onComplete?.invoke()
            return
        }

        pendingTasks.add { onComplete?.invoke() }

        if (isInitializing) return
        isInitializing = true

        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(
                    activity,
                    InitializationConfig.Builder("ca-app-pub-5550085346779978~8891033445").build()
                ) {
                    Handler(Looper.getMainLooper()).post {
                        isInitialized = true
                        isInitializing = false

                        pendingTasks.forEach { it.invoke() }
                        pendingTasks.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun runWhenReady(activity: Activity, task: () -> Unit) {
        if (isInitialized) {
            task()
        } else {
            pendingTasks.add(task)
            initialize(activity)
        }
    }

    fun isReady(): Boolean = isInitialized
}