package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ContactModel

interface OnClickContactInterface {
    fun onItemClick(contactsInfo: ContactModel.ContactItem)
}