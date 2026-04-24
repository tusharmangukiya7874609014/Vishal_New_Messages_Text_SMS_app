package com.texting.sms.messaging_app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.texting.sms.messaging_app.listener.NetworkAvailableListener

class NetworkConnectionUtil(private val context: Context) {
    private var listener: NetworkAvailableListener? = null
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var isCurrentlyConnected: Boolean? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            notifyIfChanged(true)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            notifyIfChanged(false)
        }
    }

    fun setListener(networkListener: NetworkAvailableListener) {
        this.listener = networkListener
    }

    fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, networkCallback)

        if (!isNetworkAvailable()) {
            listener?.onNetworkLost()
        }
    }

    private fun notifyIfChanged(isConnected: Boolean) {
        if (isCurrentlyConnected == isConnected) return

        isCurrentlyConnected = isConnected

        if (isConnected) {
            listener?.onNetworkAvailable()
        } else {
            listener?.onNetworkLost()
        }
    }

    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}