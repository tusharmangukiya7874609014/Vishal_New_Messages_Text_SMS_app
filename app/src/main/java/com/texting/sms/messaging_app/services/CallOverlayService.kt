package com.texting.sms.messaging_app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.activity.AfterCallBackActivity
import java.util.Locale

class CallOverlayService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private lateinit var timerRunnable: Runnable

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private var overlayView: View? = null
    private var removeView: View? = null
    private lateinit var layoutParamsView: WindowManager.LayoutParams
    private lateinit var removeParamsView: WindowManager.LayoutParams
    private lateinit var windowManagerView: WindowManager

    private var isRinging = false
    private var isOffhook = false
    private var callType: String = ""

    private var callDuration: String? = null

    companion object {
        @Volatile
        var isRunning = false
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundServiceProperly()
    }

    private fun startForegroundServiceProperly() {
        val channelId = "call_overlay"

        val channel = NotificationChannel(
            channelId,
            "Call Overlay Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("After call features")
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceProperly()

        when (intent?.getStringExtra("CALL_STATE")) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                isRinging = true
                isOffhook = false

                if (Settings.canDrawOverlays(this)) {
                    showOverlay()
                }
                startTimer()
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                isOffhook = true

                callType = if (!isRinging) {
                    "Outgoing Call"
                } else {
                    "Incoming Call"
                }

                if (!isRinging) {
                    if (Settings.canDrawOverlays(this)) {
                        showOverlay()
                    }
                    startTimer()
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (isRinging && !isOffhook) {
                    callType = "Missed Call"
                }

                isRinging = false

                if (::timerRunnable.isInitialized) {
                    handler.removeCallbacks(timerRunnable)
                }

                val hasOverlayPermission = Settings.canDrawOverlays(this)

                hideOverlay()

                if (hasOverlayPermission) {
                    try {
                        val afterCallIntent =
                            Intent(this, AfterCallBackActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                putExtra("Duration", callDuration)
                                putExtra("ClickView", 1)
                                putExtra("CallType", callType)
                            }
                        startActivity(afterCallIntent)
                    } catch (e: Exception) {
                        Log.e("ABCD", "Failed to open AfterCall screen", e)
                    }
                } else {
                    if (canUseFullScreenIntent()) {
                        showFullScreenAfterCallNotification(callDuration.toString())
                    } else {
                        showNormalHighPriorityNotification(callDuration.toString())
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun showFullScreenAfterCallNotification(duration: String) {
        val intent = Intent(this, AfterCallBackActivity::class.java).apply {
            putExtra("Duration", duration)
            putExtra("ClickView", 1)
            putExtra("CallType", callType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "call_overlay")
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Call Ended")
            .setContentText("Tap to view call details")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(3001, builder.build())
    }

    private fun showNormalHighPriorityNotification(duration: String) {
        val intent = Intent(this, AfterCallBackActivity::class.java).apply {
            putExtra("Duration", duration)
            putExtra("ClickView", 1)
            putExtra("CallType", callType)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            2002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, "call_overlay")
            .setSmallIcon(R.drawable.ic_call)
            .setContentTitle("Call Ended")
            .setContentText("Tap to view details")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(3001, builder.build())
    }

    private fun canUseFullScreenIntent(): Boolean {
        return if (Build.VERSION.SDK_INT >= 34) {
            val notificationManager =
                getSystemService(NotificationManager::class.java)
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                if (it.isAttachedToWindow) {
                    windowManagerView.removeView(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                overlayView = null
            }
        }

        removeView?.let {
            try {
                if (it.isAttachedToWindow) {
                    windowManagerView.removeView(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                removeView = null
            }
        }
    }

    private fun showOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_call_view, null)
        removeView = LayoutInflater.from(this).inflate(R.layout.overlay_remove_area, null)

        windowManagerView = getSystemService(WINDOW_SERVICE) as WindowManager

        // Bubble overlay params
        layoutParamsView = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParamsView.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        layoutParamsView.x = 0
        layoutParamsView.y = 0

        // Remove area params
        removeParamsView = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            300, // height
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        removeParamsView.gravity = Gravity.BOTTOM

        setupDrag()
        windowManagerView.addView(overlayView, layoutParamsView)
        windowManagerView.addView(removeView, removeParamsView)
        removeView?.visibility = View.GONE
    }

    private fun setupDrag() {
        val rootView = overlayView?.findViewById<CardView>(R.id.cvHorizontalView)
        val ivOtherSettings = overlayView?.findViewById<RelativeLayout>(R.id.rvOtherSettings)
        val ivNotes = overlayView?.findViewById<RelativeLayout>(R.id.rvViewMessages)
        val ivMessages = overlayView?.findViewById<RelativeLayout>(R.id.rvSendMessage)

        ivOtherSettings?.setOnClickListener {
            val intent = Intent(this, AfterCallBackActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            intent.putExtra(
                "Duration",
                callDuration
            )
            intent.putExtra("ClickView", 3)
            startActivity(intent)
        }

        ivNotes?.setOnClickListener {
            val intent = Intent(this, AfterCallBackActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            intent.putExtra(
                "Duration",
                callDuration
            )
            intent.putExtra("ClickView", 1)
            startActivity(intent)
        }

        ivMessages?.setOnClickListener {
            val intent = Intent(this, AfterCallBackActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            intent.putExtra(
                "Duration",
                callDuration
            )
            intent.putExtra("ClickView", 2)
            startActivity(intent)
        }

        rootView?.apply {
            isClickable = true
            isFocusable = true

            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParamsView.x
                        initialY = layoutParamsView.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        removeView?.visibility = View.VISIBLE
                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        layoutParamsView.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParamsView.y = initialY + (event.rawY - initialTouchY).toInt()

                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels

                        overlayView?.let {
                            val viewWidth = it.width
                            val viewHeight = it.height

                            val allowedX = viewWidth / 2
                            val allowedY = viewHeight / 2

                            layoutParamsView.x =
                                layoutParamsView.x.coerceIn(-allowedX, screenWidth - allowedX)
                            layoutParamsView.y =
                                layoutParamsView.y.coerceIn(-allowedY, screenHeight - allowedY)

                            windowManagerView.updateViewLayout(it, layoutParamsView)

                            if (isInRemoveArea(event.rawY, screenHeight)) {
                                removeView?.setBackgroundColor("#66FF0000".toColorInt())
                            } else {
                                removeView?.setBackgroundColor("#59000000".toColorInt())
                            }
                        }
                        true
                    }

                    MotionEvent.ACTION_UP -> {
                        removeView?.visibility = View.GONE

                        if (isInRemoveArea(event.rawY, resources.displayMetrics.heightPixels)) {
                            hideOverlay()
                        } else {
                            view.performClick()
                        }
                        true
                    }

                    else -> false
                }
            }
        }
    }

    private fun isInRemoveArea(touchY: Float, screenHeight: Int): Boolean {
        val removeAreaHeight = 300
        return touchY > screenHeight - removeAreaHeight
    }

    private fun startTimer() {
        seconds = 0

        timerRunnable = object : Runnable {
            override fun run() {
                seconds++
                val minutes = seconds / 60
                val sec = seconds % 60
                callDuration = String.format(Locale.ENGLISH, "%02d:%02d", minutes, sec)
                handler.postDelayed(this, 1000)
            }
        }

        handler.post(timerRunnable)
    }
}
