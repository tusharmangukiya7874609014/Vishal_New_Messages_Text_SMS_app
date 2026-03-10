package com.chat.sms_text.messages.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.children
import androidx.core.view.isNotEmpty
import androidx.core.view.isVisible
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.utils.LocaleHelper
import com.chat.sms_text.messages.utils.RootChecker

abstract class BaseActivity : AppCompatActivity() {
    private lateinit var dialog: Dialog
    private lateinit var rootOptionsDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        dialog = Dialog(this)
        rootOptionsDialog = Dialog(this)
        setupSystemUI()
    }

    override fun attachBaseContext(newBase: Context) {
        val scale = SharedPreferencesHelper.getFontScale(newBase)
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = scale

        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun getBaseContext(): Context {
        val locale = SharedPreferencesHelper.getLanguage(applicationContext)
        LocaleHelper.setLocale(this, locale)
        return super.getBaseContext()
    }

    fun View.isUserInteractionEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (this is ViewGroup && this.isNotEmpty()) {
            this.children.forEach {
                it.isUserInteractionEnabled(enabled)
            }
        }
    }

    fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    override fun onResume() {
        enableImmersiveModeWithTransparentStatusBar(this)
        if (RootChecker.isDeviceRooted()) {
            startActivity(Intent(this, RootDeviceFoundActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Setup transparent status and navigation bars by default.
     */
    private fun setupSystemUI() {
        window.apply {
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
            decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }
    }

    fun isDarkTheme(): Boolean {
        val currentNightMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun enableImmersiveModeWithTransparentStatusBar(activity: Activity) {
        val window = activity.window
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
            window.insetsController?.apply {
                setSystemBarsAppearance(
                    if (isDarkTheme()) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or if (!isDarkTheme()) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else 0
                    )
        }
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    fun View.fadeIn(duration: Long = 300) {
        if (visibility != View.VISIBLE) {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(duration)
                .setListener(null)
        }
    }

    fun View.fadeOut(duration: Long = 300) {
        if (isVisible) {
            animate()
                .alpha(0f)
                .setDuration(duration)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                    }
                })
        }
    }
}