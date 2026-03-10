package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ContactModel

interface OnRemoveForwardRecipients {
    fun onItemRemoved(contactDetails: ContactModel.ContactItem, position: Int)
}