package com.texting.sms.messaging_app.repository

import com.texting.sms.messaging_app.response.MessageTranslateResponse
import com.texting.sms.messaging_app.response.TranslateRequest
import com.texting.sms.messaging_app.utils.RetrofitClient
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