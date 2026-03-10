package com.chat.sms_text.messages.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.chat.sms_text.messages.database.ScheduledSMSDatabaseHelper
import com.chat.sms_text.messages.model.ScheduledSms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val dbHelper = ScheduledSMSDatabaseHelper(context)
                val scheduledList = dbHelper.getAllSms()
                val currentTime = System.currentTimeMillis()

                scheduledList.forEach { sms ->
                    if (sms.scheduledMillis > currentTime) {
                        scheduleSms(context, sms)
                    } else {
                        dbHelper.deleteSms(sms.id)
                    }
                }
            }
        }
    }

    private fun scheduleSms(context: Context, sms: ScheduledSms) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = ("package:" + context.packageName).toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val intent = Intent(context, SmsAlarmReceiver::class.java).apply {
            putExtra("id", sms.id)
            putExtra("number", sms.number)
            putExtra("message", sms.message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sms.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            sms.scheduledMillis,
            pendingIntent
        )
    }
}