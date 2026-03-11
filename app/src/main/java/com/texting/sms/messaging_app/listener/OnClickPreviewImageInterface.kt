package com.texting.sms.messaging_app.listener

import android.net.Uri
import com.texting.sms.messaging_app.model.ChatModel

interface OnClickPreviewImageInterface {
    fun onItemImagePreviewClick(uri: Uri)

    fun onItemTranslateClick(item: ChatModel.MessageItem, position: Int)
}