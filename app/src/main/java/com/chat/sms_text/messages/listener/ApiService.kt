package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.response.MessageLanguageListResponse
import com.chat.sms_text.messages.response.MessageTranslateResponse
import com.chat.sms_text.messages.response.TranslateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("languages.json")
    suspend fun getAllLanguagesList(): Response<MessageLanguageListResponse>

    @POST("translator.php")
    suspend fun translateSentence(
        @Body request: TranslateRequest
    ): Response<MessageTranslateResponse>
}