package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ScheduledSms

interface OnScheduledClickInterface {
    fun onItemClick(scheduledSMS : ScheduledSms)
}