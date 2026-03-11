package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.SmsBackupInfo

interface OnBackupClickInterface {
    fun onSelectBackupClick(backupName: SmsBackupInfo)
}