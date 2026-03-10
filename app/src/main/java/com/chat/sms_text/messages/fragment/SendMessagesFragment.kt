package com.chat.sms_text.messages.fragment

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.chat.sms_text.messages.databinding.FragmentSendMessagesBinding
import com.chat.sms_text.messages.activity.HomeActivity
import com.chat.sms_text.messages.activity.NewConversationActivity

class SendMessagesFragment : Fragment() {
    private var _binding: FragmentSendMessagesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendMessagesBinding.inflate(inflater, container, false)
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
        binding.rvSendMessage.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            val intent = if (isDefaultSmsApp(requireContext())) {
                Intent(requireContext(), NewConversationActivity::class.java)
            } else {
                Intent(requireContext(), HomeActivity::class.java)
            }
            startActivity(intent)
            requireActivity().finish()
        }

        binding.rvViewMessage.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            startActivity(Intent(requireContext(), HomeActivity::class.java))
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}