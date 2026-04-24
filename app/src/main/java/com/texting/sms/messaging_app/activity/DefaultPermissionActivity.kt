package com.texting.sms.messaging_app.activity

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ActivityDefaultPermissionBinding
import com.texting.sms.messaging_app.services.CallOverlayService

class DefaultPermissionActivity : BaseActivity() {
    private lateinit var binding: ActivityDefaultPermissionBinding
    private lateinit var smsDefaultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_default_permission)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initClickListener()
    }

    private fun prepareIntentLauncher() {
        smsDefaultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == RESULT_OK) {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                } else {
                    showToast(resources.getString(R.string.permission_denied_and_app_not_set_as_default_sms_app))
                }
            }
    }

    private fun initView() {
        if (Settings.canDrawOverlays(this)) {
            if (!CallOverlayService.isRunning) {
                val serviceIntent = Intent(this, CallOverlayService::class.java).apply {
                    putExtra("CALL_STATE", "10000")
                }

                try {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } catch (e: Exception) {
                    Log.d("ABCD","Overlay Service :- ${e.localizedMessage}")
                }
            }
        }

        prepareIntentLauncher()
    }

    private fun initClickListener() {
        binding.btnSetDefaultPermission.setOnClickListener {
            if (isDefaultSmsApp(this)) {
                navigateNext()
            } else requestSmsRole()
        }
    }

    private fun requestSmsRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager: RoleManager = getSystemService(RoleManager::class.java) ?: return
            val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
            if (isRoleAvailable) {
                val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                if (!isRoleHeld) {
                    smsDefaultLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS))
                }
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(
                    Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    packageName
                )
            }
            smsDefaultLauncher.launch(intent)
        }
    }

    private fun navigateNext() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}