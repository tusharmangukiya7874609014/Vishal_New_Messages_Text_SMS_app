package com.chat.sms_text.messages.repository

import com.chat.sms_text.messages.response.MessageLanguageListResponse
import com.chat.sms_text.messages.utils.RetrofitClient
import retrofit2.Response

class AllLanguageRepository {
    suspend fun fetchAllLanguages(): Response<MessageLanguageListResponse> {
        return RetrofitClient.apiService.getAllLanguagesList()
    }
}