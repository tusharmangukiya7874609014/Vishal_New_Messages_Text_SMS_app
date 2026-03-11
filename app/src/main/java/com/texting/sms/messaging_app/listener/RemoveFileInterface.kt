package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.AttachFile

interface RemoveFileInterface {
    fun onItemClick(file : AttachFile)
}