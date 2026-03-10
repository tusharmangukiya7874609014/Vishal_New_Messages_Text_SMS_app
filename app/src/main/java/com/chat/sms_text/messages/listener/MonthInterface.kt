package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.MonthFile

interface MonthInterface {
    fun onSelectedMonthClick(monthDetails : MonthFile)
}