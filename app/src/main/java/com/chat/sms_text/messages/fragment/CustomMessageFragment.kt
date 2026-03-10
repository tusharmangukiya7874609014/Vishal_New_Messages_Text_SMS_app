package com.chat.sms_text.messages.fragment

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.FragmentCustomMessageBinding
import java.net.URLEncoder

class CustomMessageFragment : Fragment() {
    private var _binding: FragmentCustomMessageBinding? = null
    private val binding get() = _binding!!
    private var quickMessage = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomMessageBinding.inflate(inflater, container, false)
        initView()
        setupRadioGroup()
        setupSendMessage()
        return binding.root
    }

    private fun initView() {
        quickMessage = resources.getString(R.string.can_t_talk_now_i_ll_call_you_back_soon)
        binding.etQuickMessages.setText(quickMessage)
    }

    private fun setupRadioGroup() {
        binding.rgQuickMessage.setOnCheckedChangeListener { group, checkedId ->
            val context = context ?: return@setOnCheckedChangeListener

            group.findViewById<RadioButton>(checkedId)?.let {
                quickMessage = it.text.toString()
                binding.etQuickMessages.setText(quickMessage)
            }

            for (i in 0 until group.childCount) {
                val rb = group.getChildAt(i) as? RadioButton ?: continue
                rb.buttonTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(
                        context,
                        if (rb.id == checkedId) R.color.app_theme_color else R.color.app_theme_color
                    )
                )
            }
        }
    }

    private fun setupSendMessage() {
        binding.ivSendMessage.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val context = context ?: return@setOnClickListener

            val finalMessage = binding.etQuickMessages.text
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: quickMessage

            Thread {
                val intents = buildMessagingIntents(context, finalMessage)

                if (intents.isNotEmpty()) {
                    requireActivity().runOnUiThread {
                        val chooser = Intent.createChooser(
                            intents.first(),
                            getString(R.string.choose_messaging_app)
                        ).apply {
                            if (intents.size > 1) {
                                putExtra(
                                    Intent.EXTRA_INITIAL_INTENTS,
                                    intents.drop(1).toTypedArray()
                                )
                            }
                        }
                        startActivity(chooser)
                    }
                }
            }.start()
        }
    }

    private fun buildMessagingIntents(
        context: Context,
        message: String
    ): List<Intent> {

        val pm = context.packageManager
        val intentList = mutableListOf<Intent>()

        runCatching {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://wa.me/?text=$encodedMsg".toUri()
                setPackage("com.whatsapp")
            }.takeIf { it.resolveActivity(pm) != null }
                ?.let(intentList::add)
        }

        val smsIntent = Intent(Intent.ACTION_SENDTO, "smsto:".toUri()).apply {
            putExtra("sms_body", message)
        }

        pm.queryIntentActivities(smsIntent, 0).forEach { info ->
            val pkg = info.activityInfo.packageName
            if (!pkg.contains("jio", true)) {
                intentList.add(Intent(smsIntent).setPackage(pkg))
            }
        }

        return intentList
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}