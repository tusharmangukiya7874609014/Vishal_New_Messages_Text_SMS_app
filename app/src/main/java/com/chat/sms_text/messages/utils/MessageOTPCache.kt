package com.chat.sms_text.messages.utils

object MessageOTPCache {
    private val cache = mutableMapOf<Int, String?>()

    fun getLatestMessageOTP(message: String): String? {
        return cache[message.hashCode()]
    }

    fun putLatestMessageOTP(message: String, otp: String?) {
        cache[message.hashCode()] = otp
    }

    fun extractOtpFromLatestMessage(text: String): String? {
        val cleanedText = text.lowercase().replace("[^a-z0-9\\s]".toRegex(), " ")

        val blacklistedKeywords =
            listOf("debit", "credited", "credit", "transaction", "rs", "inr", "amount")
        val blacklistRegex =
            Regex("\\b(${blacklistedKeywords.joinToString("|")})\\b", RegexOption.IGNORE_CASE)
        if (blacklistRegex.containsMatchIn(cleanedText)) {
            return null
        }

        val keywordRegex = Regex(
            "\\b(otp|one time password|verification code|verification|code)\\b",
            RegexOption.IGNORE_CASE
        )
        if (!keywordRegex.containsMatchIn(cleanedText)) {
            return null
        }

        val otpRegex = Regex("\\b\\d{4}\\b|\\b\\d{6}\\b")
        return otpRegex.find(cleanedText)?.value
    }
}