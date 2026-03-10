package com.chat.sms_text.messages.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chat.sms_text.messages.repository.AllLanguageRepository
import com.chat.sms_text.messages.response.Language
import kotlinx.coroutines.launch

class AllLanguagesViewModel : ViewModel() {
    private val repository = AllLanguageRepository()

    private val _allLanguages = MutableLiveData<List<Language>>()
    val allLanguages: LiveData<List<Language>> get() = _allLanguages

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadAllLanguages() {
        viewModelScope.launch {
            try {
                val response = repository.fetchAllLanguages()
                if (response.isSuccessful) {
                    val responseValue = response.body()?.status ?: false
                    val responseLanguages = response.body()?.languages ?: emptyList()

                    if (!responseValue) {
                        _error.value = "Error: ${response.message()}"
                        return@launch
                    }
                    val modelLanguages = responseLanguages.map {
                        Language(it.code, it.language)
                    }
                    _allLanguages.value = modelLanguages
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Exception: ${e.message}"
            }
        }
    }
}