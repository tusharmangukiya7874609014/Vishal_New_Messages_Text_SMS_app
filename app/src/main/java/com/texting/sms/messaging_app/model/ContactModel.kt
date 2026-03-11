package com.texting.sms.messaging_app.model

sealed class ContactModel {
    data class HeaderWithFirstItem(val title: String, val firstContact: ContactItem) :
        ContactModel()

    data class ContactItem(
        val contactId: String?,
        val name: String?,
        var phoneNumbers: String?,
        val photoUri: String?,
        var isSelectedProfile: Boolean = false
    ) : ContactModel()
}
