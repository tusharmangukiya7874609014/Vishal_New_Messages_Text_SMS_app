package com.texting.sms.messaging_app.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.texting.sms.messaging_app.model.ContactModel

object AllContactDiffCallback : DiffUtil.ItemCallback<ContactModel>() {
    override fun areItemsTheSame(
        oldItem: ContactModel,
        newItem: ContactModel
    ): Boolean {
        return when (oldItem) {
            is ContactModel.ContactItem if newItem is ContactModel.ContactItem ->
                oldItem.contactId == newItem.contactId

            is ContactModel.HeaderWithFirstItem if newItem is ContactModel.HeaderWithFirstItem ->
                oldItem.firstContact == newItem.firstContact

            else -> false
        }
    }

    override fun areContentsTheSame(
        oldItem: ContactModel,
        newItem: ContactModel
    ): Boolean = oldItem == newItem
}