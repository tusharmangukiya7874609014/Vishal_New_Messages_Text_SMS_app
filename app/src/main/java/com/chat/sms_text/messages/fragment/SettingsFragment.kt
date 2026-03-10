package com.chat.sms_text.messages.fragment

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.FragmentSettingsBinding
import com.chat.sms_text.messages.activity.HomeActivity
import com.chat.sms_text.messages.activity.NewConversationActivity

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        initClickListener()
        return binding.root
    }

    private fun isDefaultSmsApp(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager.isRoleHeld(RoleManager.ROLE_SMS)
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    private fun initClickListener() {
        binding.rvMessage.setOnClickListener {
            val intent = if (isDefaultSmsApp(requireContext())) {
                Intent(requireContext(), NewConversationActivity::class.java)
            } else {
                Intent(requireContext(), HomeActivity::class.java)
            }
            startActivity(intent)
            requireActivity().finish()
        }

        binding.rvEditContact.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val intent =
                Intent(ContactsContract.Intents.Insert.ACTION).setType(ContactsContract.RawContacts.CONTENT_TYPE)
            startActivity(intent)
        }

        binding.rvSendMail.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
            }
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(), getString(R.string.no_email_app_found), Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.rvCalender.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(), getString(R.string.no_calendar_app_found), Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.rvWeb.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val url = "https://www.google.com"
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}