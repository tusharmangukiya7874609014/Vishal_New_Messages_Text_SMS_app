package com.texting.sms.messaging_app.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityPrivacyPolicyBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PrivacyPolicyActivity : BaseActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_privacy_policy)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.root.post {
            fetchPrivacyPolicyUrl()
        }
        initView()
        initClickListener()
    }

    private fun initView() {
        applySpannableTerms()
    }

    private fun fetchPrivacyPolicyUrl() {
        FirebaseDatabase.getInstance()
            .getReference("messageManager")
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.child("privacyPolicy")
                        .getValue(String::class.java)
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            SharedPreferencesHelper.saveString(
                                this@PrivacyPolicyActivity,
                                Const.PRIVACY_URL,
                                it
                            )
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PrivacyPolicy", error.message)
                }
            })
    }

    private fun applySpannableTerms() {
        val privacyText = "Privacy policy"
        val fullText = "I agree to the $privacyText of App"

        val spannable = SpannableString(fullText)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = SharedPreferencesHelper.getString(
                    this@PrivacyPolicyActivity,
                    Const.PRIVACY_URL,
                    ""
                ).ifEmpty { "https://sites.google.com/view/messagespro-smschat/home" }

                if (url.isNotBlank()) {
                    openCustomTabSafely(url)
                } else {
                    showToast(getString(R.string.something_went_wrong))
                }
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = true
                ds.color = ContextCompat.getColor(
                    this@PrivacyPolicyActivity,
                    R.color.app_theme_color
                )
            }
        }

        val start = fullText.indexOf(privacyText)
        if (start >= 0) {
            spannable.setSpan(
                clickableSpan,
                start,
                start + privacyText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        binding.txtPrivacyPolicy.apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    private fun openCustomTabSafely(url: String) {
        try {
            val params = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(
                    ContextCompat.getColor(this, R.color.app_theme_color)
                )
                .build()

            val intent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(params)
                .setShowTitle(true)
                .build()

            intent.launchUrl(this, url.toUri())

        } catch (_: Exception) {
            openInBrowserFallback(url)
        }
    }

    private fun openInBrowserFallback(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        } catch (_: ActivityNotFoundException) {
            showToast(getString(R.string.no_browser_found))
        }
    }

    private fun initClickListener() {
        binding.btnAccept.setOnClickListener {
            if (!binding.checkPrivacyPolicy.isChecked) {
                showToast(
                    getString(R.string.please_accept_the_privacy_policy_and_terms_conditions)
                )
                return@setOnClickListener
            }

            SharedPreferencesHelper.saveBoolean(
                this,
                Const.IS_ACCEPT_PRIVACY_POLICY,
                value = true
            )

            startActivity(
                Intent(this, DefaultPermissionActivity::class.java)
            )
            finish()
        }
    }
}