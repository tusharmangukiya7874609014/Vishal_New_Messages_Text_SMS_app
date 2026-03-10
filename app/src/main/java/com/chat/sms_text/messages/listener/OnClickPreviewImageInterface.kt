package com.chat.sms_text.messages.listener

import android.net.Uri
import com.chat.sms_text.messages.model.ChatModel

interface OnClickPreviewImageInterface {
    fun onItemImagePreviewClick(uri: Uri)

    fun onItemTranslateClick(item: ChatModel.MessageItem, position: Int)
}