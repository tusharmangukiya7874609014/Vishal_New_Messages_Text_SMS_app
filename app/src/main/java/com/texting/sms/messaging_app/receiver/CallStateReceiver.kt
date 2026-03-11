package com.texting.sms.messaging_app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.services.CallOverlayService

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            if (context == null) return
            if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

            if (!Settings.canDrawOverlays(context) && !isFullScreenNotificationAllowed(context)) return

            val isEnablePostCallScreen =
                SharedPreferencesHelper.getBoolean(context, Const.ENABLE_AFTER_CALL_SCREEN, true)

            if (!isEnablePostCallScreen) return

            val serviceIntent = Intent(context, CallOverlayService::class.java).apply {
                putExtra("CALL_STATE", state)
            }

            try {
                context.startService(serviceIntent)
            } catch (e: Exception) {
                Log.e("CallOverlay", "Failed to start overlay service", e)
            }
        }
    }

    private fun isFullScreenNotificationAllowed(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.canUseFullScreenIntent()
        } else true
    }
}

