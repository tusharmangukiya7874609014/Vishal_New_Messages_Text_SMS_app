package com.texting.sms.messaging_app.model

data class ScheduledSms(
    val id: Int = 0,
    val name: String,
    val number: String,
    val message: String,
    val imageURIs: String,
    val time: String,
    val contactUserPhotoUri : String,
    val threadID: String,
    val scheduledMillis: Long
)