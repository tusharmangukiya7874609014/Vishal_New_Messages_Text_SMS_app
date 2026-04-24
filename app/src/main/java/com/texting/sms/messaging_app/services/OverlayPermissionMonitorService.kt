package com.texting.sms.messaging_app.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.google.firebase.analytics.FirebaseAnalytics
import com.texting.sms.messaging_app.activity.HomeActivity
import com.texting.sms.messaging_app.activity.LanguageActivity
import com.texting.sms.messaging_app.database.SharedPreferencesHelper

class OverlayPermissionMonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval = 200L

    private val runnable = object : Runnable {
        override fun run() {
            if (Settings.canDrawOverlays(this@OverlayPermissionMonitorService)) {
                val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
                    this@OverlayPermissionMonitorService,
                    "IS_FIRST_TIME_LANGUAGE_SELECTED",
                    false
                )

                val intent = if (!isFirstTimeLanguageSelected) {
                    firebaseLogEvent(
                        this@OverlayPermissionMonitorService, "OVERLAY_PERMISSION_PAGE", "OVERLAY_PERMISSION_ALLOWED"
                    )
                    Intent(this@OverlayPermissionMonitorService, LanguageActivity::class.java)
                } else {
                    firebaseLogEvent(
                        this@OverlayPermissionMonitorService, "OVERLAY_PERMISSION_PAGE", "OVERLAY_PERMISSION_ALLOWED"
                    )
                    SharedPreferencesHelper.saveBoolean(
                        this@OverlayPermissionMonitorService,
                        "NO_ADS_APP_OPEN",
                        true
                    )
                    Intent(this@OverlayPermissionMonitorService, HomeActivity::class.java)
                }

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                stopSelf()
                return
            }

            val isIntentOfFullScreenNotification = SharedPreferencesHelper.getBoolean(
                this@OverlayPermissionMonitorService,
                "IS_FULL_SCREEN_NOTIFICATION",
                false
            )

            if (isFullScreenNotificationAllowed() && isIntentOfFullScreenNotification) {
                val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
                    this@OverlayPermissionMonitorService,
                    "IS_FIRST_TIME_LANGUAGE_SELECTED",
                    false
                )

                val intent = if (!isFirstTimeLanguageSelected) {
                    firebaseLogEvent(
                        this@OverlayPermissionMonitorService, "OVERLAY_PERMISSION_PAGE", "FULL_SCREEN_NOTIFICATION_ALLOWD"
                    )

                    Intent(this@OverlayPermissionMonitorService, LanguageActivity::class.java)
                } else {
                    firebaseLogEvent(
                        this@OverlayPermissionMonitorService, "OVERLAY_PERMISSION_PAGE", "FULL_SCREEN_NOTIFICATION_ALLOWD"
                    )
                    SharedPreferencesHelper.saveBoolean(
                        this@OverlayPermissionMonitorService,
                        "NO_ADS_APP_OPEN",
                        true
                    )
                    Intent(this@OverlayPermissionMonitorService, HomeActivity::class.java)
                }

                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)

                SharedPreferencesHelper.saveBoolean(
                    this@OverlayPermissionMonitorService,
                    "IS_FULL_SCREEN_NOTIFICATION",
                    false
                )

                stopSelf()
                return
            }
            handler.postDelayed(this, interval)
        }
    }

    fun firebaseLogEvent(
        context: Context, eventName: String, paramValue: String
    ) {
        val bundle = Bundle().apply {
            putString("page_name", paramValue)
        }

        FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle)
    }

    private fun isFullScreenNotificationAllowed(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else true
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        handler.post(runnable)
    }

    override fun onDestroy() {
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }
}
