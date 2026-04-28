package com.texting.sms.messaging_app.model

import com.texting.sms.messaging_app.R

enum class SwipeAction(val resId: Int) {
    NONE(R.string.none),
    ARCHIVED(R.string.archive),
    DELETE(R.string.delete),
    CALL(R.string.call),
    MARK_AS_READ(R.string.mark_read),
    MARK_AS_UNREAD(R.string.mark_unread),
    ADD_TO_PRIVATE_CHAT(R.string.add_to_private_chat);

    companion object {
        fun fromStoredValue(value: String?): SwipeAction? =
            runCatching { value?.let { valueOf(it) } }.getOrDefault(NONE)
    }
}