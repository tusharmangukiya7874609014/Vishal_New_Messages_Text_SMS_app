package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.response.MessageLanguageListResponse
import com.texting.sms.messaging_app.response.MessageTranslateResponse
import com.texting.sms.messaging_app.response.TranslateRequest
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