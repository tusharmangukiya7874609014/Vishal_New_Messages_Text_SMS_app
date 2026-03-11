package com.texting.sms.messaging_app.listener

interface NetworkListener {
    fun onNetworkChange(isConnected: Boolean)
}