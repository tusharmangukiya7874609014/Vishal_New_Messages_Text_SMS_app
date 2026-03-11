package com.texting.sms.messaging_app.utils

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    override fun doWork(): Result {
        val message = NotificationStore.getNextMessage(applicationContext)
        NotificationHelper.showNotification(applicationContext, "Messages : SMS & Texting", message)

        WorkScheduler.scheduleNext(applicationContext)

        return Result.success()
    }
}