package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ScheduledSms

interface OnScheduledClickInterface {
    fun onItemClick(scheduledSMS : ScheduledSms)
}