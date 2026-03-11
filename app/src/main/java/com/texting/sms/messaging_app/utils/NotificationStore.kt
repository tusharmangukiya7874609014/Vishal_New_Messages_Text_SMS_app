package com.texting.sms.messaging_app.utils

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper

object NotificationStore {

    private val defaultMessages = listOf(
        "Set Messages as your Default SMS app to unlock spam protection & smart inbox 📩.",
        "Good morning 👋 You have 2 unread messages waiting in your Smart Inbox.",
        "We filtered 5 spam messages today 🚫. Stay protected with Messages.",
        "Your OTPs are neatly organized 🔐. No need to search anymore.",
        "Reply faster with one-tap Quick Replies 🚀. Try it now!",
        "1 week with Messages 🎉. You’ve sent 25 messages. Keep the streak going!",
        "Did you know? You can customize your inbox with Dark Mode 🌙 & Themes 🎨.",
        "Important alert: Your bank SMS has been marked as Priority 🔔.",
        "We blocked 18 spam SMS this week 🛡. You’re safer with Messages.",
        "2 weeks streak 💪 You’ve stayed protected & organized. Keep it up!",
        "Schedule birthday wishes 🎂 & reminders easily with Scheduled SMS.",
        "Tip: Auto-delete old OTPs after 7 days 🧹. Save space, stay clutter-free.",
        "Achievement unlocked 🏆: 100 messages sent using Messages.",
        "1 Month Milestone 🎉 You’ve saved 50 spam blocks & 30 OTPs. Thanks for trusting Messages 🙏.",
        "2 unread messages waiting 📩.",
        "We blocked 12 spam messages for you this week 🛡",
        "100 messages sent this week 🎉"
    )

    private var remoteMessages: List<String> = defaultMessages

    fun initDatabase() {
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("messageManager").child("randomNotificationContent")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                for (child in snapshot.children) {
                    val value = child.getValue(String::class.java)
                    if (!value.isNullOrBlank()) {
                        list.add(value)
                    }
                }

                if (list.isNotEmpty()) {
                    remoteMessages = list
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ABCD","Firebase Error :- $error")
            }
        })
    }

    fun getNextMessage(context: Context): String {
        val lastIndex = SharedPreferencesHelper.getInt(context, Const.LAST_INDEX, -1)
        val nextIndex = (lastIndex + 1) % remoteMessages.size
        SharedPreferencesHelper.saveInt(context, Const.LAST_INDEX, nextIndex)
        return remoteMessages[nextIndex]
    }
}