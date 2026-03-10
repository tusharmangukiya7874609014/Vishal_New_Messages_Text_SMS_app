package com.chat.sms_text.messages.listener

interface NetworkListener {
    fun onNetworkChange(isConnected: Boolean)
}