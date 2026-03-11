package com.texting.sms.messaging_app.activity

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ActivityPermissionOverlayBinding

class PermissionOverlayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionOverlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_permission_overlay)
        initView()
        initClickListener()
    }

    private fun isDarkMode(context: Context): Boolean {
        val nightModeFlags =
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun initView() {
        if (isDarkMode(this)) {
            binding.rvBottomView.setBackgroundResource(R.color.black)
            binding.txtTitle.setTextColor(getColor(R.color.white))
            binding.txtAppName.setTextColor(getColor(R.color.white))
        } else {
            binding.rvBottomView.setBackgroundResource(R.color.white)
            binding.txtTitle.setTextColor(getColor(R.color.black))
            binding.txtAppName.setTextColor(getColor(R.color.black))
        }
    }

    private fun initClickListener() {
        binding.viewOfBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}