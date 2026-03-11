package com.texting.sms.messaging_app.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.model.ChatUser
import com.texting.sms.messaging_app.repository.MessageRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MessageRemoteViewsFactory(private val context: Context) :
    RemoteViewsService.RemoteViewsFactory {

    private var messageList: List<ChatUser> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        messageList = MessageRepository.getLatestMessages()
    }

    override fun onDestroy() {
        messageList = emptyList()
    }

    override fun getCount(): Int {
        return if (messageList.isEmpty()) 1 else messageList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (messageList.isNotEmpty()) {
            val chat = messageList[position]
            val views = RemoteViews(context.packageName, R.layout.widget_message_item)

            if (messageList.isEmpty() || position >= messageList.size) {
                return views
            }

            views.setTextViewText(R.id.name, chat.contactName ?: chat.address)
            views.setTextViewText(R.id.message, chat.latestMessage)
            views.setImageViewResource(R.id.photo, R.drawable.img_plus)

            views.setTextViewText(
                R.id.time,
                formatMessageTime(chat.timestamp)
            )

            if (isDarkTheme(context)) {
                views.setTextColor(
                    R.id.name,
                    ContextCompat.getColor(context, R.color.dark_theme_title_color)
                )
                views.setTextColor(
                    R.id.message,
                    ContextCompat.getColor(context, R.color.dark_theme_sub_title_color)
                )
                views.setTextColor(
                    R.id.time,
                    ContextCompat.getColor(context, R.color.dark_theme_sub_title_color)
                )
                views.setInt(
                    R.id.widget_container,
                    "setBackgroundColor",
                    ContextCompat.getColor(context, R.color.dark_theme_main_color)
                )
            } else {
                views.setTextColor(
                    R.id.name,
                    ContextCompat.getColor(context, R.color.light_theme_title_color)
                )
                views.setTextColor(
                    R.id.message,
                    ContextCompat.getColor(context, R.color.light_theme_sub_title_color)
                )
                views.setTextColor(
                    R.id.time,
                    ContextCompat.getColor(context, R.color.light_theme_sub_title_color)
                )
                views.setInt(
                    R.id.widget_container,
                    "setBackgroundColor",
                    ContextCompat.getColor(context, R.color.light_theme_main_color)
                )
            }

            if (chat.unreadCount > 0) {
                views.setTextViewTextSize(R.id.name, TypedValue.COMPLEX_UNIT_SP, 15f)
                views.setTextViewTextSize(R.id.message, TypedValue.COMPLEX_UNIT_SP, 12f)
                views.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_SP, 12f)
                if (isDarkTheme(context)) {
                    views.setTextColor(
                        R.id.name,
                        ContextCompat.getColor(context, R.color.white)
                    )
                    views.setTextColor(
                        R.id.message,
                        ContextCompat.getColor(context, R.color.white)
                    )
                    views.setTextColor(
                        R.id.time,
                        ContextCompat.getColor(context, R.color.white)
                    )
                } else {
                    views.setTextColor(
                        R.id.name,
                        ContextCompat.getColor(context, R.color.light_theme_title_color)
                    )
                    views.setTextColor(
                        R.id.message,
                        ContextCompat.getColor(context, R.color.light_theme_title_color)
                    )
                    views.setTextColor(
                        R.id.time,
                        ContextCompat.getColor(context, R.color.light_theme_title_color)
                    )
                }
            }

            if (!chat.photoUri.isNullOrEmpty()) {
                try {
                    val profileBitmap =
                        chat.photoUri?.let { uriStringToBitmap(context, it) }
                    views.setImageViewBitmap(
                        R.id.photo,
                        profileBitmap?.let { getRoundedBitmap(it) })
                } catch (_: Exception) {
                    views.setImageViewResource(R.id.photo, R.drawable.ic_profile_view)
                }
            } else {
                views.setImageViewResource(R.id.photo, R.drawable.ic_profile_view)
            }
            return views
        } else {
            val views = RemoteViews(context.packageName, R.layout.widget_message_item)
            views.setTextViewText(R.id.name, "No messages")
            return views
        }
    }

    fun isDarkTheme(context: Context): Boolean {
        val currentNightMode =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun getRoundedBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)

        val paint = Paint()
        paint.isAntiAlias = true

        val rect = Rect(0, 0, size, size)
        val rectF = RectF(rect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, null, rect, paint)

        return output
    }

    private fun uriStringToBitmap(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = uriString.toUri()
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun formatMessageTime(timestamp: Long): String {
        val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when {
            // Today: show time like 4:40 PM
            now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(timestamp))
            }

            // Same week: show day name like Mon, Tue
            now.get(Calendar.WEEK_OF_YEAR) == messageCal.get(Calendar.WEEK_OF_YEAR) &&
                    now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(timestamp))
            }

            // Same month: show like May 10
            now.get(Calendar.MONTH) == messageCal.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(timestamp))
            }

            // Different year: show full date like 10/05/2023
            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(timestamp))
            }
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long =
        if (messageList.isNotEmpty()) messageList[position].threadId else 0

    override fun hasStableIds(): Boolean = true
}

