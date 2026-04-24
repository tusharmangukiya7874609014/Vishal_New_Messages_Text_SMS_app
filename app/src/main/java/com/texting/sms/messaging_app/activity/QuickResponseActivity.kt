package com.texting.sms.messaging_app.activity

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.adapter.QuickResponseAdapter
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ActivityQuickResponseBinding
import com.texting.sms.messaging_app.databinding.DialogDeleteConversationBinding
import com.texting.sms.messaging_app.databinding.DialogEditQuickMessageBinding
import com.texting.sms.messaging_app.listener.OnQuickMessageInterface
import com.texting.sms.messaging_app.model.QuickResponse

class QuickResponseActivity : BaseActivity(), OnQuickMessageInterface {
    private lateinit var binding: ActivityQuickResponseBinding
    private lateinit var rvQuickResponseAdapter: QuickResponseAdapter
    private lateinit var quickMessageResponseList: MutableList<QuickResponse>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quick_response)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        initClickListener()
    }

    private fun initView() {
        quickMessageResponseList = SharedPreferencesHelper.getQuickMessageList(this).toMutableList()

        if (quickMessageResponseList.isEmpty()) {
            quickMessageResponseList = listOf(
                QuickResponse("❤\uFE0F"),
                QuickResponse("\uD83D\uDE00"),
                QuickResponse("\uD83D\uDC4D"),
                QuickResponse("\uD83D\uDE4F"),
                QuickResponse("\uD83D\uDE0E"),
                QuickResponse("\uD83D\uDE02"),
                QuickResponse("Hi \uD83D\uDC4B"),
                QuickResponse("How are you?"),
                QuickResponse("Okay \uD83D\uDC4D"),
                QuickResponse("Thanks \uD83D\uDE4F"),
            ).toMutableList()
            SharedPreferencesHelper.saveQuickMessageList(this, quickMessageResponseList)
        }

        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvQuickResponseList.setLayoutManager(layoutManager)
        rvQuickResponseAdapter = QuickResponseAdapter(this, quickMessageResponseList, this)
        binding.rvQuickResponseList.adapter = rvQuickResponseAdapter
        (binding.rvQuickResponseList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations =
            false
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivAddIntoTheList.setOnClickListener {
            showEditOrAddQuickMessageActions("AddQuickMessage")
        }
    }

    override fun onQuickMessageDeleteClick(message: QuickResponse, position: Int) {
        showDeleteQuickMessageDialog(position)
    }

    override fun onQuickMessageEditClick(message: QuickResponse, position: Int) {
        showEditOrAddQuickMessageActions("EditQuickMessage", message.message, position = position)
    }

    private fun showDeleteQuickMessageDialog(position: Int) {
        val dialog = Dialog(this)
        val deleteQuickMessageDialogBinding: DialogDeleteConversationBinding =
            DialogDeleteConversationBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(deleteQuickMessageDialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(true)

        deleteQuickMessageDialogBinding.txtStatement.text =
            getString(R.string.are_you_sure_you_want_to_delete_this_quick_response)

        deleteQuickMessageDialogBinding.btnNever.setOnClickListener {
            dialog.dismiss()
        }

        deleteQuickMessageDialogBinding.btnYes.setOnClickListener {
            quickMessageResponseList.removeAt(position)
            val finalQuickMessageList = quickMessageResponseList.toMutableList()
            SharedPreferencesHelper.saveQuickMessageList(this, finalQuickMessageList)
            rvQuickResponseAdapter.updateList(finalQuickMessageList)
            dialog.dismiss()
        }

        if (!isFinishing && !isDestroyed) {
            val window = dialog.window
            val params = window?.attributes
            val displayMetrics = resources.displayMetrics
            params?.width = (displayMetrics.widthPixels * 0.9).toInt()
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            dialog.show()
        }
    }

    private fun showEditOrAddQuickMessageActions(
        performActions: String,
        message: String = "",
        position: Int = -1
    ) {
        val dialog = Dialog(this)
        val quickMessageEditOrAddDialogBinding: DialogEditQuickMessageBinding =
            DialogEditQuickMessageBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(quickMessageEditOrAddDialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        if (performActions.contentEquals("EditQuickMessage") && message.isNotEmpty()) {
            quickMessageEditOrAddDialogBinding.txtTitle.text =
                resources.getString(R.string.update_quick_response)
            quickMessageEditOrAddDialogBinding.btnApply.text = resources.getString(R.string.apply)
            quickMessageEditOrAddDialogBinding.etAddQuickMessage.setText(message)
        } else {
            quickMessageEditOrAddDialogBinding.txtTitle.text =
                getString(R.string.add_quick_response)
            quickMessageEditOrAddDialogBinding.btnApply.text = getString(R.string.add)
        }

        quickMessageEditOrAddDialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        quickMessageEditOrAddDialogBinding.btnApply.setOnClickListener {
            val newMessage = quickMessageEditOrAddDialogBinding.etAddQuickMessage.text.toString()
            if (newMessage.isNotEmpty()) {
                if (performActions.contentEquals("EditQuickMessage") && position >= 0) {
                    quickMessageResponseList[position] = QuickResponse(newMessage)
                } else {
                    quickMessageResponseList.add(QuickResponse(newMessage))
                }

                val finalQuickMessageList = quickMessageResponseList.toMutableList()
                SharedPreferencesHelper.saveQuickMessageList(this, finalQuickMessageList)
                rvQuickResponseAdapter.updateList(finalQuickMessageList)
                dialog.dismiss()
            } else {
                showToast(getString(R.string.please_enter_quick_message))
            }
        }

        dialog.setCancelable(true)
        if (!isFinishing && !isDestroyed) {
            val window = dialog.window
            val params = window?.attributes
            val displayMetrics = resources.displayMetrics
            params?.width = (displayMetrics.widthPixels * 0.9).toInt()
            params?.height = WindowManager.LayoutParams.WRAP_CONTENT
            window?.attributes = params
            dialog.show()
        }
    }
}