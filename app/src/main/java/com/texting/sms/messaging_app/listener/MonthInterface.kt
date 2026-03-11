package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.MonthFile

interface MonthInterface {
    fun onSelectedMonthClick(monthDetails : MonthFile)
}