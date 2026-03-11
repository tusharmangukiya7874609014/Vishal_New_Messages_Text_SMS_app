package com.texting.sms.messaging_app.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityCustomLunchBinding
import com.texting.sms.messaging_app.utils.RootChecker

class CustomLunchActivity : BaseActivity() {
    private lateinit var binding: ActivityCustomLunchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_custom_lunch)
        binding.root.post {
            navigateNext()
        }
    }

    private fun navigateNext() {
        val isFirstTimeLanguageSelected = SharedPreferencesHelper.getBoolean(
            this,
            "IS_FIRST_TIME_LANGUAGE_SELECTED",
            false
        )

        val nextIntent = when {
            RootChecker.isDeviceRooted() -> {
                Intent(this, RootDeviceFoundActivity::class.java)
            }

            !hasRequiredPermissions() -> {
                Intent(this, ReadPermissionActivity::class.java)
            }

            !isOverlayPermissionGranted() -> {
                Intent(this, OverlayPermissionActivity::class.java)
            }

            !isFirstTimeLanguageSelected -> {
                Intent(this, LanguageActivity::class.java)
            }

            else -> {
                Intent(this, HomeActivity::class.java)
            }
        }

        startActivity(nextIntent)
        finish()
    }

    private fun hasRequiredPermissions(): Boolean {
        val notificationGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        val readPhoneStateGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

        return notificationGranted && readPhoneStateGranted
    }

    private fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }
}