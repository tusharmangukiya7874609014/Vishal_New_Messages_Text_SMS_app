package com.chat.sms_text.messages.listener

interface OnArchivedRemoveInterface {
    fun onUpdateTheRecyclerView(isRemoved: Boolean)
}

object CallbackHolder {
    var listener: OnArchivedRemoveInterface? = null
    var highlightViewListener : OnHighlightView? = null
}