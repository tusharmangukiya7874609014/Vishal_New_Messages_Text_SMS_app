package com.chat.sms_text.messages.utils

import android.os.Build
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.collections.any
import kotlin.text.contains

object RootChecker {

    fun isDeviceRooted(): Boolean {
        return checkBuildTags() || checkSuperUserApk() || checkSuBinary() || checkRootCommand()
    }

    // ✅ Method 1: Check for Test Keys in Build Tags (Common in Rooted Devices)
    private fun checkBuildTags(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    // ✅ Method 2: Check if SuperUser APK Exists
    private fun checkSuperUserApk(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk"
        )
        return paths.any { File(it).exists() }
    }

    // ✅ Method 3: Check if "su" Binary Exists
    private fun checkSuBinary(): Boolean {
        val suPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/usr/we-need-root/su-backup",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su"
        )
        return suPaths.any { File(it).exists() }
    }

    // ✅ Method 4: Try Executing "su" Command
    private fun checkRootCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val input = BufferedReader(InputStreamReader(process.inputStream))
            input.readLine() != null
        } catch (e: Exception) {
            false
        }
    }
}
