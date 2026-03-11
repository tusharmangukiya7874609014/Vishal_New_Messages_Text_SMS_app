package com.texting.sms.messaging_app.listener

import com.texting.sms.messaging_app.model.ChatWallpaper

interface OnChatWallpaperClickInterface {
    fun onChatWallpaperClick(position: Int, color: ChatWallpaper)
}