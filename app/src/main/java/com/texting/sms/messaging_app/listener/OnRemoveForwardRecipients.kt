package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ContactModel

interface OnRemoveForwardRecipients {
    fun onItemRemoved(contactDetails: ContactModel.ContactItem, position: Int)
}