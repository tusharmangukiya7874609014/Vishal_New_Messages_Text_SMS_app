package com.texting.sms.messaging_app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.texting.sms.messaging_app.repository.MessageTranslateRepository
import com.texting.sms.messaging_app.response.MessageTranslateResponse
import kotlinx.coroutines.launch

class MessageTranslateViewModel : ViewModel() {
    private val repository = MessageTranslateRepository()

    private val _translateMessage = MutableLiveData<MessageTranslateResponse>()
    val translateMessage: LiveData<MessageTranslateResponse> get() = _translateMessage

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun translateMessage(changeLanguageCode: String, sentence: String) {
        viewModelScope.launch {
            try {
                val response = repository.fetchTranslateMessage(
                    changeLanguageCode = changeLanguageCode,
                    sentence = sentence
                )
                if (response.isSuccessful) {
                    _translateMessage.value = response.body()
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Exception: ${e.message}"
            }
        }
    }
}