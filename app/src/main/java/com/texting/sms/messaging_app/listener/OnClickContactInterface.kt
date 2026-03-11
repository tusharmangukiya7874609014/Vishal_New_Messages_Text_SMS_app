package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ContactModel

interface OnClickContactInterface {
    fun onItemClick(contactsInfo: ContactModel.ContactItem)
}