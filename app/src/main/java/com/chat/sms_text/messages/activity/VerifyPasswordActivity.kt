package com.chat.sms_text.messages.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ActivityVerifyPasswordBinding
import com.chat.sms_text.messages.listener.OnClickKeyboardInterface
import com.chat.sms_text.messages.model.Password
import com.chat.sms_text.messages.viewmodel.KeyboardViewModel
import com.chat.sms_text.messages.adapter.KeyboardAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class VerifyPasswordActivity : BaseActivity(), OnClickKeyboardInterface {
    private lateinit var binding: ActivityVerifyPasswordBinding
    private lateinit var viewModel: KeyboardViewModel
    private lateinit var rvKeyboardAdapter: KeyboardAdapter
    private lateinit var numberList: List<Password>
    private var verifyPassword = ""
    private var isNavigationHandled = false
    private var isModifyPassword = false
    private var isModifySecurityQuestions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_verify_password)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initObserver()
        initClickListener()
    }

    private fun initView() {
        if (intent.hasExtra(Const.IS_MODIFY_PASSWORD)) {
            binding.txtForgotPassword.visibility = View.GONE
            isModifyPassword = intent.getBooleanExtra(Const.IS_MODIFY_PASSWORD, false)
        }

        if (intent.hasExtra(Const.IS_MODIFY_SECURITY_QUESTION)) {
            binding.txtForgotPassword.visibility = View.GONE
            isModifySecurityQuestions =
                intent.getBooleanExtra(Const.IS_MODIFY_SECURITY_QUESTION, false)
        }

        viewModel = ViewModelProvider(this)[KeyboardViewModel::class.java]

        numberList = listOf(
            Password("1"),
            Password("2"),
            Password("3"),
            Password("4"),
            Password("5"),
            Password("6"),
            Password("7"),
            Password("8"),
            Password("9"),
        )

        val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(this, 3)
        binding.rvNumberView.setLayoutManager(layoutManager)
        rvKeyboardAdapter =
            KeyboardAdapter(mutableListOf(), this)
        binding.rvNumberView.adapter = rvKeyboardAdapter

        if (numberList.isNotEmpty()) {
            rvKeyboardAdapter.updateData(numberList)
        }
    }

    private fun initObserver() {
        viewModel.liveValue.observe(this) { value ->
            binding.selectedValue = value.length
        }

        viewModel.clearFlag.observe(this) { value ->
            if (value) {
                rvKeyboardAdapter.onClearView()
                binding.isZeroSelected = false

                if (verifyPassword.isNotEmpty()) {
                    verifyPassword = verifyPassword.dropLast(1)
                    binding.selectedValue = verifyPassword.length
                }
            }
        }
    }

    private fun initClickListener() {
        binding.rvZero.setOnClickListener {
            binding.isZeroSelected = true
            rvKeyboardAdapter.onClearView()
            verifyPasswordManage("0")
        }

        binding.rvClear.setOnClickListener {
            viewModel.clearValue(true)
        }

        binding.txtForgotPassword.setOnClickListener {
            val intent = Intent(this, SecurityQuestionsActivity::class.java)
            intent.putExtra(Const.IS_MODIFY_PASSWORD, true)
            startActivity(intent)
        }
    }

    override fun onItemClick(password: Password, position: Int) {
        binding.isZeroSelected = false
        rvKeyboardAdapter.updateSelectedNumber(position)
        verifyPasswordManage(password.number)
    }

    private fun verifyPasswordManage(number: String?) {
        if (verifyPassword.length >= 4 || number == null) return

        verifyPassword += number
        viewModel.updateValue(verifyPassword)

        if (verifyPassword.length < 4) return

        val storedPassword = SharedPreferencesHelper.getString(
            this,
            Const.FINAL_PASSWORD,
            Const.STRING_DEFAULT_VALUE
        )

        if (verifyPassword == storedPassword) {
            showToast(getString(R.string.password_verify_successfully))
            navigateAfterDelay()
        } else {
            lifecycleScope.launch {
                delay(300)
                resetPinUI()
            }
            showToast(getString(R.string.password_wrong_try_again))
        }
    }

    private fun navigateAfterDelay() {
        lifecycleScope.launch {
            delay(500)
            resetPinUI()
            handleNavigation()
        }
    }

    private fun handleNavigation() {
        if (isNavigationHandled) return
        isNavigationHandled = true

        when {
            isModifyPassword -> {
                startActivity(
                    Intent(this, SetPasswordActivity::class.java).apply {
                        putExtra(Const.IS_MODIFY_PASSWORD, true)
                    }
                )
                finish()
            }

            isModifySecurityQuestions -> {
                startActivity(
                    Intent(this, SecurityQuestionsActivity::class.java).apply {
                        putExtra(Const.IS_CHANGE_SECURITY_QUESTION, true)
                    }
                )
                finish()
            }

            else -> {
                val intent = Intent(this, PrivateChatListActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun resetPinUI() {
        verifyPassword = ""
        binding.isZeroSelected = false
        binding.selectedValue = 0
        rvKeyboardAdapter.onClearView()
    }
}