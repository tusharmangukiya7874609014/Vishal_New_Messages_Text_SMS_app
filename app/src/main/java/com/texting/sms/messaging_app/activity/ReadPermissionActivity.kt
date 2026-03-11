package com.texting.sms.messaging_app.activity

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityReadPermissionBinding
import com.texting.sms.messaging_app.databinding.DialogPermissionDeniedBinding

class ReadPermissionActivity : BaseActivity() {
    private lateinit var binding: ActivityReadPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_read_permission)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initClickListener()
    }

    private fun showPermissionsDeniedDialog() {
        val dialog = Dialog(this)
        val permissionDeniedDialogBinding: DialogPermissionDeniedBinding =
            DialogPermissionDeniedBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(permissionDeniedDialogBinding.root)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)

            val metrics = resources.displayMetrics
            window.attributes = window.attributes.apply {
                width = (metrics.widthPixels * 0.9f).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        permissionDeniedDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        permissionDeniedDialogBinding.btnSettings.setOnClickListener {
            dialog.dismiss()
            openAppSettings()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                SharedPreferencesHelper.saveInt(this, "NOTIFICATION_PERMISSION_DENIED", 0)
                requestPhonePermissions()
            } else {
                val deniedCount =
                    (SharedPreferencesHelper.getInt(this, "NOTIFICATION_PERMISSION_DENIED", 0) + 1)

                SharedPreferencesHelper.saveInt(this, "NOTIFICATION_PERMISSION_DENIED", deniedCount)

                if (deniedCount >= 2) {
                    showPermissionsDeniedDialog()
                } else {
                    showToast(getString(R.string.notification_permission_denied))
                }
            }
        }

    private val phonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                SharedPreferencesHelper.saveInt(this, "READ_PHONE_STATE_PERMISSION_DENIED", 0)
                startActivity(Intent(this, OverlayPermissionActivity::class.java))
                finish()
            } else {
                val deniedCount = (SharedPreferencesHelper.getInt(
                    this,
                    "READ_PHONE_STATE_PERMISSION_DENIED",
                    0
                ) + 1)
                SharedPreferencesHelper.saveInt(
                    this,
                    "READ_PHONE_STATE_PERMISSION_DENIED",
                    deniedCount
                )

                if (deniedCount >= 2) {
                    showPermissionsDeniedDialog()
                } else {
                    showToast(getString(R.string.phone_permissions_denied))
                }
            }
        }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPhonePermissions()
            }
        } else {
            requestPhonePermissions()
        }
    }

    private fun requestPhonePermissions() {
        val phonePermissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE
        )

        val notGranted = phonePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            phonePermissionLauncher.launch(notGranted.toTypedArray())
        } else {
            startActivity(Intent(this, OverlayPermissionActivity::class.java))
            finish()
        }
    }

    private fun initClickListener() {
        binding.btnAllowPermission.setOnClickListener {
            requestNotificationPermission()
        }
    }
}