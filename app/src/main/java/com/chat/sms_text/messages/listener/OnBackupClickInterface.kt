package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.SmsBackupInfo

interface OnBackupClickInterface {
    fun onSelectBackupClick(backupName: SmsBackupInfo)
}