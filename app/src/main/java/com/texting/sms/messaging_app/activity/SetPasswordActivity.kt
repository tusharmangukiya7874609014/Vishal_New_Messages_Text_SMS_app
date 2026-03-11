package com.texting.sms.messaging_app.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivitySetPasswordBinding
import com.texting.sms.messaging_app.listener.OnClickKeyboardInterface
import com.texting.sms.messaging_app.model.Password
import com.texting.sms.messaging_app.viewmodel.KeyboardViewModel
import com.texting.sms.messaging_app.adapter.KeyboardAdapter

class SetPasswordActivity : BaseActivity(), OnClickKeyboardInterface {
    private lateinit var binding: ActivitySetPasswordBinding
    private lateinit var rvKeyboardAdapter: KeyboardAdapter
    private lateinit var numberList: List<Password>
    private var setPassword = ""
    private var confirmPassword = ""
    private lateinit var viewModel: KeyboardViewModel
    private var flagPassword = false
    private var isModifyPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_set_password)
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
        viewModel = ViewModelProvider(this)[KeyboardViewModel::class.java]

        if (intent.hasExtra(Const.IS_MODIFY_PASSWORD)) {
            isModifyPassword = intent.getBooleanExtra(Const.IS_MODIFY_PASSWORD, false)
        }

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

                if (!flagPassword) {
                    if (setPassword.isNotEmpty()) {
                        setPassword = setPassword.dropLast(1)
                        binding.selectedValue = setPassword.length
                    }
                } else {
                    if (confirmPassword.isNotEmpty()) {
                        confirmPassword = confirmPassword.dropLast(1)
                        binding.selectedValue = confirmPassword.length
                    }
                }
            }
        }
    }

    private fun initClickListener() {
        binding.rvZero.setOnClickListener {
            binding.isZeroSelected = true
            rvKeyboardAdapter.onClearView()
            if (!flagPassword) {
                handleSetPassword("0")
            } else {
                handleConfirmPassword("0")
            }
        }

        binding.rvClear.setOnClickListener {
            viewModel.clearValue(true)
        }
    }

    override fun onItemClick(password: Password, position: Int) {
        binding.isZeroSelected = false
        rvKeyboardAdapter.updateSelectedNumber(position)
        if (!flagPassword) {
            handleSetPassword(password.number)
        } else {
            handleConfirmPassword(password.number)
        }
    }

    private fun handleSetPassword(number: String?) {
        if (setPassword.length >= 4) return

        setPassword += number
        viewModel.updateValue(setPassword)

        if (setPassword.length == 4) {
            Handler(Looper.getMainLooper()).postDelayed({
                flagPassword = true
                binding.isZeroSelected = false
                binding.selectedValue = 0
                rvKeyboardAdapter.onClearView()
                binding.txtSubTitle.text = getString(R.string.confirm_your_password)
            }, 200)
        }
    }

    private fun handleConfirmPassword(number: String?) {
        if (confirmPassword.length >= 4) return

        confirmPassword += number
        viewModel.updateValue(confirmPassword)

        if (confirmPassword.length == 4) {
            validatePassword()
        }
    }

    private fun validatePassword() {
        if (setPassword != confirmPassword) {
            showToast(getString(R.string.pin_not_created))
            Handler(Looper.getMainLooper()).postDelayed({
                resetPasswordState()
            }, 300)
            return
        }

        savePassword()

        if (isModifyPassword) {
            showToast(getString(R.string.password_modified_successfully))
        } else {
            showToast(getString(R.string.pin_set_successfully))
        }

        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 1000)
    }

    private fun savePassword() {
        SharedPreferencesHelper.saveString(this, Const.FINAL_PASSWORD, confirmPassword)
    }

    private fun resetPasswordState() {
        setPassword = ""
        confirmPassword = ""
        flagPassword = false

        binding.isZeroSelected = false
        rvKeyboardAdapter.onClearView()
        binding.selectedValue = 0

        binding.txtTitle.visibility = View.VISIBLE
        binding.txtSubTitle.text = getString(R.string.enter_your_password)
    }

    private fun navigateNext() {
        if (!isModifyPassword) {
            val intent = Intent(this, SecurityQuestionsActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else {
            finish()
            return
        }
    }
}