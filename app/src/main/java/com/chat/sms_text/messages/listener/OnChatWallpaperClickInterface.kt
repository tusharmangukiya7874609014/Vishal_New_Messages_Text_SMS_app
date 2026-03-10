package com.chat.sms_text.messages.listener

import com.chat.sms_text.messages.model.ChatWallpaper

interface OnChatWallpaperClickInterface {
    fun onChatWallpaperClick(position: Int, color: ChatWallpaper)
}