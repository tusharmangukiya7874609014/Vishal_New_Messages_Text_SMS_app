package com.chat.sms_text.messages.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class KeyboardViewModel : ViewModel() {
    private val mutableLiveValue = MutableLiveData<String>()
    private val mutableBooleanLiveValue = MutableLiveData<Boolean>()

    val liveValue: LiveData<String> get() = mutableLiveValue
    val clearFlag: LiveData<Boolean> get() = mutableBooleanLiveValue

    fun updateValue(value: String) {
        mutableLiveValue.value = value
    }

    fun clearValue(value: Boolean) {
        mutableBooleanLiveValue.value = value
    }
}