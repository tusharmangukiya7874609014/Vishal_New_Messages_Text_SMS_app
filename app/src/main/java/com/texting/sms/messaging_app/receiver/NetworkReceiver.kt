package com.texting.sms.messaging_app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import com.texting.sms.messaging_app.listener.NetworkListener

class NetworkReceiver(private val listener: NetworkListener) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            if (context == null || intent == null) return
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val networkInfo = cm?.activeNetworkInfo
            val isConnected = networkInfo?.isConnectedOrConnecting == true
            listener.onNetworkChange(isConnected)
        } catch (e: Exception) {
            Log.e("NetworkReceiver", "Error receiving network broadcast", e)
        }
    }
}