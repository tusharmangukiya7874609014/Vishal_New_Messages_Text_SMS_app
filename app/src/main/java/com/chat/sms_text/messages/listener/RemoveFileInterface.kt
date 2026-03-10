package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.AttachFile

interface RemoveFileInterface {
    fun onItemClick(file : AttachFile)
}