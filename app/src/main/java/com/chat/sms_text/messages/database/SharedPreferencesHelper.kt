package com.chat.sms_text.messages.database

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.core.content.edit
import com.chat.sms_text.messages.model.AppTheme
import com.chat.sms_text.messages.model.ChatUser
import com.chat.sms_text.messages.model.QuickResponse
import com.chat.sms_text.messages.utils.StarCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject

object SharedPreferencesHelper {
    private const val PREF_NAME = "MyPreferences"

    fun saveString(context: Context, key: String, value: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        sharedPreferences.edit { putString(key, value) }
    }

    fun getString(context: Context, key: String, defaultValue: String): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    fun saveInt(context: Context, key: String, value: Int) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        sharedPreferences.edit { putInt(key, value) }
    }

    fun getInt(context: Context, key: String, defaultValue: Int): Int {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun saveLong(context: Context, key: String, value: Long) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        sharedPreferences.edit { putLong(key, value) }
    }

    fun getLong(context: Context, key: String, defaultValue: Long): Long {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun saveFloat(context: Context, key: String, value: Float) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        sharedPreferences.edit { putFloat(key, value) }
    }

    fun getFloat(context: Context, key: String, defaultValue: Float): Float {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun saveBoolean(context: Context, key: String, value: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        sharedPreferences.edit { putBoolean(key, value) }
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**  For Language Localization  **/
    fun setLanguage(context: Context, language: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(Const.SELECTED_LANGUAGE, language)
        }
    }

    fun getLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(Const.SELECTED_LANGUAGE, "en") ?: "en"
    }

    fun saveJsonToPreferences(context: Context, key: String, jsonObject: JSONObject) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(key, jsonObject.toString())
        }
    }

    fun getJsonFromPreferences(context: Context, key: String): JSONObject {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString(key, null)
        return JSONObject(jsonString.toString())
    }

    /**  For Archived Thread Save, get All Archived Threads, get All Unarchived Threads, check specific thread is Archived or Unarchived and remove specific thread from Archived  **/
    fun saveArchivedThread(context: Context, threadId: Long) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        val archivedIds =
            sharedPreferences.getStringSet(Const.ARCHIVED_IDS_KEY, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        archivedIds.add(threadId.toString())
        sharedPreferences.edit { putStringSet(Const.ARCHIVED_IDS_KEY, archivedIds) }
    }

    fun getArchivedThreadIds(context: Context): Set<String> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getStringSet(Const.ARCHIVED_IDS_KEY, emptySet()) ?: emptySet()
    }

    fun getArchivedThreads(context: Context, allThreads: List<ChatUser>): List<ChatUser> {
        val archivedIds = getArchivedThreadIds(context)
        return allThreads.filter { archivedIds.contains(it.threadId.toString()) }
    }

    fun filterUnarchivedThreads(context: Context, allThreads: List<ChatUser>): List<ChatUser> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val archivedIds =
            sharedPreferences.getStringSet(Const.ARCHIVED_IDS_KEY, emptySet()) ?: emptySet()
        return allThreads.filterNot { archivedIds.contains(it.threadId.toString()) }
    }

    fun removeArchivedThread(context: Context, threadId: Long) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        val archivedIds =
            sharedPreferences.getStringSet(Const.ARCHIVED_IDS_KEY, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        archivedIds.remove(threadId.toString())
        sharedPreferences.edit { putStringSet(Const.ARCHIVED_IDS_KEY, archivedIds) }
    }

    fun isThreadArchived(context: Context, threadId: Long): Boolean {
        return getArchivedThreadIds(context).contains(threadId.toString())
    }

    /**  For Private and Secure Thread Save, get All Private Threads, get All Non-private Threads, check specific thread is Private or Non-private and remove specific thread from Private  **/
    fun savePrivateThread(context: Context, threadId: Long) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        val privateIds =
            sharedPreferences.getStringSet(Const.PRIVATE_IDS_KEY, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        privateIds.add(threadId.toString())
        sharedPreferences.edit { putStringSet(Const.PRIVATE_IDS_KEY, privateIds) }
    }

    fun getPrivateThreadIds(context: Context): Set<String> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getStringSet(Const.PRIVATE_IDS_KEY, emptySet()) ?: emptySet()
    }

    fun getPrivateThreads(context: Context, allThreads: List<ChatUser>): List<ChatUser> {
        val privateIds = getPrivateThreadIds(context)
        return allThreads.filter { privateIds.contains(it.threadId.toString()) }
    }

    fun filterNonPrivateThreads(context: Context, allThreads: List<ChatUser>): List<ChatUser> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val privateIds =
            sharedPreferences.getStringSet(Const.PRIVATE_IDS_KEY, emptySet()) ?: emptySet()
        return allThreads.filterNot { privateIds.contains(it.threadId.toString()) }
    }

    fun removePrivateThread(context: Context, threadId: Long) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        val privateIds =
            sharedPreferences.getStringSet(Const.PRIVATE_IDS_KEY, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        privateIds.remove(threadId.toString())
        sharedPreferences.edit { putStringSet(Const.PRIVATE_IDS_KEY, privateIds) }
    }

    fun isThreadPrivate(context: Context, threadId: Long): Boolean {
        return getPrivateThreadIds(context).contains(threadId.toString())
    }

    /**  For Pinned Or Unpinned and check specific thread is pinned or not **/
    fun isPinned(context: Context, threadId: Long): Boolean {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        return sharedPreferences.getBoolean("pin_$threadId", false)
    }

    fun setPinned(context: Context, threadId: Long, pinned: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_MULTI_PROCESS)
        sharedPreferences.edit { putBoolean("pin_$threadId", pinned) }
    }

    fun setUseSystemFont(context: Context, useSystemFont: Boolean) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putBoolean(Const.KEY_USE_SYSTEM_FONT, useSystemFont) }
    }

    fun useSystemFont(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(Const.KEY_USE_SYSTEM_FONT, false)
    }

    /** Increments click count and checks if ad should be shown */
    fun shouldShowAd(context: Context, counter: Int): Boolean {
        val interstitialAdsMinutesCount =
            getLong(
                context,
                Const.INTERSTITIAL_ADS_MINUTE_COUNT,
                60000L
            )
        val currentTime = System.currentTimeMillis()

        val lastShownTime = getLong(
            context,
            "LAST_AD_SHOWN_TIME",
            0L
        )
        var currentCount = getInt(
            context,
            Const.IS_INTERSTITIAL_COUNT,
            0
        )

        if (getBoolean(
                context,
                Const.IS_FIRST_TIME_APP_OPEN,
                true
            )
        ) {
            saveBoolean(
                context,
                Const.IS_FIRST_TIME_APP_OPEN,
                false
            )
            saveLong(
                context,
                "LAST_AD_SHOWN_TIME",
                currentTime
            )
            saveInt(
                context,
                Const.IS_INTERSTITIAL_COUNT,
                0
            )
            startCooldownTimer(interstitialAdsMinutesCount)
            return true
        }

        if (currentTime - lastShownTime < interstitialAdsMinutesCount) {
            return false
        }

        currentCount += 1
        saveInt(
            context,
            Const.IS_INTERSTITIAL_COUNT,
            currentCount
        )

        // ✅ After 1 minute passed
        return if (currentCount >= counter) {
            saveInt(
                context,
                Const.IS_INTERSTITIAL_COUNT,
                0
            )
            saveLong(
                context,
                "LAST_AD_SHOWN_TIME",
                currentTime
            )
            startCooldownTimer(interstitialAdsMinutesCount)
            true
        } else {
            false
        }
    }

    private fun startCooldownTimer(durationMillis: Long) {
        object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                Log.d("CooldownTimer", "Timer : $secondsLeft")
            }

            override fun onFinish() {
                Log.d("CooldownTimer", "Finish")
            }
        }.start()
    }

    /** For a Dark, Light and System Default App Theme  **/
    fun saveTheme(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putString(Const.APP_THEME, theme.name) }
    }

    fun getSavedTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(Const.APP_THEME, AppTheme.LIGHT.name)
        return AppTheme.valueOf(name ?: AppTheme.LIGHT.name)
    }

    fun saveArrayList(context: Context, key: String, list: ArrayList<String>) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            val gson = Gson()
            val json = gson.toJson(list)
            putString(key, json)
        }
    }

    fun getArrayList(context: Context, key: String): ArrayList<String> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString(key, null)
        val type = object : TypeToken<ArrayList<String>>() {}.type
        return if (json != null) {
            gson.fromJson(json, type)
        } else {
            ArrayList()
        }
    }

    /**  For Font Size in Settings App  **/
    fun setFontScale(context: Context, scale: Float) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putFloat(Const.FONT_SCALE_KEY, scale) }
    }

    fun getFontScale(context: Context): Float {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(Const.FONT_SCALE_KEY, 1.1f)
    }

    /**  For Spam & Blocked List to add, remove and check specific thread is blocked or unblocked and to select a clear blocked list   **/
    fun addToBlockList(context: Context, address: String) {
        val normalized = address.trim().uppercase()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet(Const.BLOCKED_SET_KEY, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        if (!blocked.contains(normalized)) {
            blocked.add(normalized)
            prefs.edit { putStringSet(Const.BLOCKED_SET_KEY, blocked) }
        }
    }

    fun removeFromBlockList(context: Context, address: String) {
        val normalized = address.trim().uppercase()
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet(Const.BLOCKED_SET_KEY, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
        if (blocked.remove(normalized)) {
            prefs.edit { putStringSet(Const.BLOCKED_SET_KEY, blocked) }
        }
    }

    fun isBlocked(context: Context, address: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val blocked = prefs.getStringSet(Const.BLOCKED_SET_KEY, emptySet()) ?: emptySet()
        return blocked.contains(address.trim().uppercase())
    }

    fun getAllBlocked(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Const.BLOCKED_SET_KEY, emptySet()) ?: emptySet()
    }

    fun clearBlockedList(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { remove(Const.BLOCKED_SET_KEY) }
    }

    /**  For a deleted thread when app is opened and clear delete thread list **/
    fun addDeletedThreadId(context: Context, threadId: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val deleted = prefs.getStringSet(Const.DELETED_THREADS_KEY, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()

        if (deleted.add(threadId.toString())) {
            prefs.edit { putStringSet(Const.DELETED_THREADS_KEY, deleted) }
        }
    }

    fun getDeletedThreadIds(context: Context): Set<Long> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Const.DELETED_THREADS_KEY, setOf())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: setOf()
    }

    fun removeDeletedThreadId(context: Context, threadId: Long) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val deleted =
            prefs.getStringSet(Const.DELETED_THREADS_KEY, mutableSetOf())?.toMutableSet() ?: return

        if (deleted.remove(threadId.toString())) {
            prefs.edit { putStringSet(Const.DELETED_THREADS_KEY, deleted) }
        }
    }

    fun clearDeletedThreadIds(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                remove(Const.DELETED_THREADS_KEY)
            }
    }

    fun saveQuickMessageList(context: Context, audioList: List<QuickResponse>) {
        val prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
        prefs.edit {
            val gson = Gson()
            val json = gson.toJson(audioList)
            putString(Const.QUICK_RESPONSE_MESSAGE_LIST, json)
        }
    }

    fun getQuickMessageList(context: Context): List<QuickResponse> {
        val prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
        val json = prefs.getString(Const.QUICK_RESPONSE_MESSAGE_LIST, null)
        return if (json != null) {
            val type = object : TypeToken<List<QuickResponse>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun getKeyForCategory(category: StarCategory, threadId: String): String {
        return when (category) {
            StarCategory.TEXT_ONLY -> "starred_text_$threadId"
            StarCategory.LINK -> "starred_link_$threadId"
            StarCategory.IMAGE -> "starred_image_$threadId"
        }
    }

    fun getStarredMessages(
        context: Context,
        category: StarCategory,
        threadId: String
    ): MutableSet<String> {
        val key = getKeyForCategory(category, threadId)
        val prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
        return prefs.getStringSet(key, mutableSetOf())?.toMutableSet()
            ?: mutableSetOf()
    }

    fun isMessageStarred(
        context: Context,
        messageId: String,
        category: StarCategory,
        threadId: String
    ): Boolean {
        return getStarredMessages(context, category, threadId).contains(messageId)
    }

    fun toggleStar(
        context: Context,
        messageId: String,
        category: StarCategory,
        threadId: String
    ) {
        val prefs = context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )
        val starred = getStarredMessages(context, category, threadId)

        if (starred.contains(messageId)) {
            starred.remove(messageId)
        } else {
            starred.add(messageId)
        }

        prefs.edit { putStringSet(getKeyForCategory(category, threadId), starred) }
    }

    fun deleteThreadStarredMessages(
        context: Context,
        category: StarCategory,
        threadId: String
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(getKeyForCategory(category, threadId))
        }
    }

    fun deleteSpecificStarredMessage(
        context: Context,
        messageId: String,
        category: StarCategory,
        threadId: String
    ) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val starred = getStarredMessages(context, category, threadId)

        if (starred.contains(messageId)) {
            starred.remove(messageId)
            prefs.edit {
                putStringSet(getKeyForCategory(category, threadId), starred)
            }
        }
    }
}