package com.texting.sms.messaging_app.model

data class SmsBackupInfo(
    val fileName: String,
    val backupTime: String,
    val totalMessages: Int,
    val fileSize: String,
    val filePath: String
)
