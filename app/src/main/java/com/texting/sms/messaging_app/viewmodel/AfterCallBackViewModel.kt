package com.texting.sms.messaging_app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AfterCallBackViewModel : ViewModel() {

    private val _selectedTab = MutableLiveData(1)
    val selectedTab: LiveData<Int> = _selectedTab

    fun selectTab(tab: Int) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
    }
}