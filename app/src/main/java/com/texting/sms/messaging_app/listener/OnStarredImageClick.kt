package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.MmsContactInfo

interface OnStarredImageClick {
    fun onImagesClick(messageDetails: String, actionOfTask : String, messageDetailsView: MmsContactInfo)
}