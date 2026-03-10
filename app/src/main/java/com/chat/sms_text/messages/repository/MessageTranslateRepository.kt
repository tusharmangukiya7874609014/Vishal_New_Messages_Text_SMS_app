package com.chat.sms_text.messages.repository

import com.chat.sms_text.messages.response.MessageTranslateResponse
import com.chat.sms_text.messages.response.TranslateRequest
import com.chat.sms_text.messages.utils.RetrofitClient
import retrofit2.Response

class MessageTranslateRepository {
    suspend fun fetchTranslateMessage(
        changeLanguageCode: String,
        sentence: String
    ): Response<MessageTranslateResponse> {
        val request = TranslateRequest(
            selectedLanguage = changeLanguageCode,
            messageBody = sentence
        )
        return RetrofitClient.apiService.translateSentence(request)
    }
}