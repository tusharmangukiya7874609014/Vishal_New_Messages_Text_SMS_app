package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.Password

interface OnClickKeyboardInterface {
    fun onItemClick(password: Password, position: Int)
}