package com.texting.sms.messaging_app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.WAP_PUSH_RECEIVED") {
            Log.e("ABCD", "MMS Received")
        }
    }
}