package com.chat.sms_text.messages.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.ActivityRootDeviceFoundBinding

class RootDeviceFoundActivity : BaseActivity() {
    private lateinit var binding: ActivityRootDeviceFoundBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_root_device_found)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initClickListener()
    }

    private fun initClickListener() {
        binding.txtClose.setOnClickListener {
            showToast(getString(R.string.closing_app_for_security_reasons))

            Handler(Looper.getMainLooper()).postDelayed({
                finishAffinity()
            }, 1500)
        }

        binding.btnRootManager.setOnClickListener {
            try {
                // ✅ Function to Open Root Manager Settings
                val rootApps = listOf(
                    "eu.chainfire.supersu",     // SuperSU
                    "com.topjohnwu.magisk",     // Magisk
                    "com.kingroot.kinguser"     // KingRoot
                )

                for (packageName in rootApps) {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        startActivity(intent)
                        return@setOnClickListener
                    }
                    showToast(getString(R.string.no_root_manager_found))
                }
            } catch (_: Exception) {
                showToast(getString(R.string.unable_to_open_root_manager_options))
            }
        }
    }
}