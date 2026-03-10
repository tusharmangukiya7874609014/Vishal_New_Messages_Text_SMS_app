package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.MmsContactInfo

interface OnStarredImageClick {
    fun onImagesClick(messageDetails: String, actionOfTask : String, messageDetailsView: MmsContactInfo)
}