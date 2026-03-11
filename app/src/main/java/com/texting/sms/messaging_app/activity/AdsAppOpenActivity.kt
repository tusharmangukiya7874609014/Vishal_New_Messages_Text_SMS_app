package com.texting.sms.messaging_app.activity

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.utils.MyApplication
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ActivityAdsAppOpenBinding

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