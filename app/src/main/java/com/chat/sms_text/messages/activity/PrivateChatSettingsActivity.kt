package com.chat.sms_text.messages.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ActivityPrivateChatSettingsBinding
import com.chat.sms_text.messages.utils.getColorFromAttr

class PrivateChatSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityPrivateChatSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_private_chat_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initClickListener()
    }

    private fun initView() {
        if (SharedPreferencesHelper.getBoolean(this, Const.PRIVATE_CHAT_NOTIFICATION, true)) {
            binding.switchNotifications.isChecked = true
            binding.switchNotifications.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
        } else {
            binding.switchNotifications.isChecked = false
            binding.switchNotifications.setBackColor(
                ColorStateList.valueOf(
                    getColorFromAttr(
                        R.attr.switchColor
                    )
                )
            )
        }
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.rvAddContacts.setOnClickListener {
            startActivity(Intent(this, AllConversationsActivity::class.java))
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesHelper.saveBoolean(this, Const.PRIVATE_CHAT_NOTIFICATION, isChecked)
            if (isChecked) {
                binding.switchNotifications.setBackColor(ColorStateList.valueOf(getColor(R.color.app_theme_color)))
            } else {
                binding.switchNotifications.setBackColor(
                    ColorStateList.valueOf(
                        getColorFromAttr(
                            R.attr.switchColor
                        )
                    )
                )
            }
        }

        binding.rvModifySecurityQue.setOnClickListener {
            val intent = Intent(this, VerifyPasswordActivity::class.java)
            intent.putExtra(Const.IS_MODIFY_SECURITY_QUESTION, true)
            startActivity(intent)
        }

        binding.rvModifyPassword.setOnClickListener {
            val intent = Intent(this, VerifyPasswordActivity::class.java)
            intent.putExtra(Const.IS_MODIFY_PASSWORD, true)
            startActivity(intent)
        }
    }
}