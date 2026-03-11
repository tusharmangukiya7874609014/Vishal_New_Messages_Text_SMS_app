package com.texting.sms.messaging_app.listener

interface OnArchivedRemoveInterface {
    fun onUpdateTheRecyclerView(isRemoved: Boolean)
}

object CallbackHolder {
    var listener: OnArchivedRemoveInterface? = null
    var highlightViewListener : OnHighlightView? = null
}