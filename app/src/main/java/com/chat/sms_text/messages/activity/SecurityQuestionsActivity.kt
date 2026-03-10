package com.chat.sms_text.messages.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ActivitySecurityQuestionsBinding
import com.chat.sms_text.messages.listener.OnClickQuestionInterface
import com.chat.sms_text.messages.model.Question
import com.chat.sms_text.messages.adapter.SecurityQuestionAdapter

class SecurityQuestionsActivity : BaseActivity(), OnClickQuestionInterface {
    private lateinit var binding: ActivitySecurityQuestionsBinding
    private lateinit var rvQuestionsAdapter: SecurityQuestionAdapter
    private lateinit var questionsList: List<Question>
    private var isModifyPassword = false
    private var isChangeSecurityQuestion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_security_questions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initClickListener()
    }

    private fun initView() {
        if (intent.hasExtra(Const.IS_MODIFY_PASSWORD)) {
            isModifyPassword = intent.getBooleanExtra(Const.IS_MODIFY_PASSWORD, false)
            binding.txtTitle.text = getString(R.string.verify_security_questions)
            binding.btnChange.text = getString(R.string.verify)
        }

        if (intent.hasExtra(Const.IS_CHANGE_SECURITY_QUESTION)) {
            isChangeSecurityQuestion =
                intent.getBooleanExtra(Const.IS_CHANGE_SECURITY_QUESTION, false)
        }
        listOfQuestions()
    }

    private fun listOfQuestions() {
        questionsList = listOf(
            Question(getString(R.string.what_was_your_childhood_nickname)),
            Question(getString(R.string.what_was_your_childhood_best_friend_s_name)),
            Question(getString(R.string.what_was_your_childhood_school_name)),
            Question(getString(R.string.what_was_your_primary_school_name)),
        )

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvQuestionsList.setLayoutManager(layoutManager)
        rvQuestionsAdapter = SecurityQuestionAdapter(questionsList, this, this)
        binding.rvQuestionsList.adapter = rvQuestionsAdapter
    }

    private fun initClickListener() {
        binding.rvQuestionListView.setOnClickListener {
            if (!binding.rvQuestionsList.isVisible) {
                binding.rvAnswer.visibility = View.INVISIBLE
                binding.rvQuestionsList.fadeIn()
            }
        }

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnChange.setOnClickListener {
            handleSecurityAnswer()
        }
    }

    override fun onItemClick(question: String) {
        binding.rvQuestionsList.visibility = View.GONE
        binding.rvAnswer.visibility = View.VISIBLE
        binding.txtQuestion.text = question
    }

    private fun handleSecurityAnswer() {
        val answer = binding.etAnswer.text?.toString()?.trim().orEmpty()
        val selectedQuestion = binding.txtQuestion.text?.toString()?.trim().orEmpty()

        if (answer.isEmpty()) {
            showToast(getString(R.string.please_enter_answer))
            return
        }

        if (isModifyPassword) {
            validateSecurityAnswer(selectedQuestion, answer)
        } else {
            saveSecurityQuestion(selectedQuestion, answer)
            navigateAfterSave()
        }
    }

    private fun saveSecurityQuestion(question: String, answer: String) {
        SharedPreferencesHelper.saveString(this, Const.SECURITY_QUESTION, question)
        SharedPreferencesHelper.saveString(this, Const.SECURITY_QUESTION_ANSWER, answer)
    }

    private fun validateSecurityAnswer(question: String, answer: String) {
        val savedQuestion = SharedPreferencesHelper.getString(
            this,
            Const.SECURITY_QUESTION,
            Const.STRING_DEFAULT_VALUE
        )

        val savedAnswer = SharedPreferencesHelper.getString(
            this,
            Const.SECURITY_QUESTION_ANSWER,
            Const.STRING_DEFAULT_VALUE
        )

        if (question == savedQuestion && answer == savedAnswer) {
            openSetPassword()
        } else {
            showToast(getString(R.string.please_enter_correct_security_question_and_answer))
        }
    }

    private fun openSetPassword() {
        val intent = Intent(this, SetPasswordActivity::class.java)
        intent.putExtra(Const.IS_MODIFY_PASSWORD, true)
        startActivity(intent)
        finish()
    }

    private fun navigateAfterSave() {
        if (!isChangeSecurityQuestion) {
            startActivity(Intent(this, PrivateChatListActivity::class.java))
        }
        finish()
    }
}