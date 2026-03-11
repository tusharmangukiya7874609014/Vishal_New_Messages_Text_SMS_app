package com.texting.sms.messaging_app.utils

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object WorkScheduler {
    fun scheduleNext(context: Context) {
        val minDelay = 24 * 60 * 60 * 1000L
        val randomExtra = Random.nextLong(0, 24 * 60 * 60 * 1000L)
        val totalDelay = minDelay + randomExtra

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(totalDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    fun initFirstSchedule(context: Context) {
        if (!SharedPreferencesHelper.getBoolean(context = context, key = "scheduled", false)) {
            scheduleNext(context)
            SharedPreferencesHelper.saveBoolean(context, "scheduled", true)
        }
    }
}