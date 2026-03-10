package com.chat.sms_text.messages.model

data class SmsBackupInfo(
    val fileName: String,
    val backupTime: String,
    val totalMessages: Int,
    val fileSize: String,
    val filePath: String
)
