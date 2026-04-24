package com.texting.sms.messaging_app.listener

interface NetworkAvailableListener {
    fun onNetworkAvailable()
    fun onNetworkLost()
}