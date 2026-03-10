package com.chat.sms_text.messages.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.ActivityAdsAppOpenBinding
import com.chat.sms_text.messages.utils.MyApplication

class AdsAppOpenActivity : AppCompatActivity() {
    private lateinit var binding : ActivityAdsAppOpenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_ads_app_open)

        val app = application as MyApplication

        app.appOpenAdManager.showAdIfAvailable(this) {
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })
    }
}