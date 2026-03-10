package com.chat.sms_text.messages.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.chat.sms_text.messages.utils.MessageRemoteViewsFactory

class MessageWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MessageRemoteViewsFactory(this.applicationContext)
    }
}