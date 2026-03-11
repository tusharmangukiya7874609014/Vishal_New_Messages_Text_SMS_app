package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.MessageFilter

interface OnMessageFilterInterface {
    fun onFilterClick(filter: MessageFilter)
}