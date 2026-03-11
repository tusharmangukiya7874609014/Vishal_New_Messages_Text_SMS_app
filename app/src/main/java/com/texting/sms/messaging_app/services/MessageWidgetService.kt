package com.texting.sms.messaging_app.services

import android.content.Intent
import android.widget.RemoteViewsService
import com.texting.sms.messaging_app.utils.MessageRemoteViewsFactory

class MessageWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MessageRemoteViewsFactory(this.applicationContext)
    }
}