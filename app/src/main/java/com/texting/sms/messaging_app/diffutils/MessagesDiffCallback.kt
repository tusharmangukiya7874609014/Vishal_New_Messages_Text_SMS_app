package com.texting.sms.messaging_app.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.texting.sms.messaging_app.model.ChatUser

object MessagesDiffCallback : DiffUtil.ItemCallback<ChatUser>() {

    override fun areItemsTheSame(
        oldItem: ChatUser,
        newItem: ChatUser
    ): Boolean {
        return oldItem.threadId == newItem.threadId
    }

    override fun areContentsTheSame(
        oldItem: ChatUser,
        newItem: ChatUser
    ): Boolean {
        return oldItem == newItem
    }
}