package com.texting.sms.messaging_app.response

import com.google.gson.annotations.SerializedName

data class TranslateRequest(
    @SerializedName("targeted_lang") val selectedLanguage: String,
    @SerializedName("text") val messageBody: String
)