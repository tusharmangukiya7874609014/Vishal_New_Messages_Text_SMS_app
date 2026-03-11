package com.texting.sms.messaging_app.repository

import com.texting.sms.messaging_app.response.MessageLanguageListResponse
import com.texting.sms.messaging_app.utils.RetrofitClient
import retrofit2.Response

class AllLanguageRepository {
    suspend fun fetchAllLanguages(): Response<MessageLanguageListResponse> {
        return RetrofitClient.apiService.getAllLanguagesList()
    }
}