package com.chat.sms_text.messages.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.chat.sms_text.messages.activity.NewConversationActivity
import com.chat.sms_text.messages.repository.MessageRepository
import com.chat.sms_text.messages.services.MessageWidgetService
import androidx.core.net.toUri
import com.chat.sms_text.messages.R

class MessageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.e("Widget","onUpdate")
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.e("Widget","onEnabled")
        MessageRepository.fetchMessages(context)
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val intent = Intent(context, MessageWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = "content://com.messages.sms/widget/$appWidgetId".toUri()
            }

            val views = RemoteViews(context.packageName, R.layout.widget_messages).apply {
                setRemoteAdapter(R.id.widget_list_view, intent)
                setImageViewResource(R.id.ivAddIcon, R.drawable.img_plus)

                val bgColor = if (isDarkTheme(context)) {
                    ContextCompat.getColor(context, R.color.dark_theme_main_color)
                } else {
                    ContextCompat.getColor(context, R.color.light_theme_main_color)
                }
                setInt(R.id.widget_list_view, "setBackgroundColor", bgColor)
            }

            val intentAdd = Intent(context, NewConversationActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intentAdd,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.ivAddIcon, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun isDarkTheme(context: Context): Boolean {
            val currentNightMode =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return currentNightMode == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
