package com.chat.sms_text.messages.activity

import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.adapter.BlockedListAdapter
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.databinding.ActivityBlockedNumberBinding
import com.chat.sms_text.messages.databinding.DialogBlockNumberBinding
import com.chat.sms_text.messages.listener.UnblockUserInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedNumberActivity : BaseActivity(), UnblockUserInterface {
    private lateinit var binding: ActivityBlockedNumberBinding
    private lateinit var rvBlockListAdapter: BlockedListAdapter
    private lateinit var blockedList: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_blocked_number)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
        getBlockedNumbersList()
        initClickListener()
    }

    private fun getBlockedNumbersList() {
        CoroutineScope(Dispatchers.IO).launch {
            blockedList = getBlockedNumbers(this@BlockedNumberActivity)

            withContext(Dispatchers.Main) {
                if (blockedList.isEmpty()) {
                    binding.rvBlockedNumber.visibility = View.GONE
                    binding.rvNoMessageView.fadeIn()
                } else {
                    binding.rvNoMessageView.visibility = View.GONE
                    rvBlockListAdapter.updateBlockedList(blockedList)
                    binding.rvBlockedNumber.fadeIn()
                }
            }
        }
    }

    private fun initView() {
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        binding.rvBlockedNumber.setLayoutManager(layoutManager)
        rvBlockListAdapter = BlockedListAdapter(mutableListOf(), this)
        binding.rvBlockedNumber.adapter = rvBlockListAdapter
    }

    private fun initClickListener() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivAddBlockedNumber.setOnClickListener {
            showBlockOrUnblockDialog(Const.STRING_DEFAULT_VALUE)
        }
    }

    private fun getBlockedNumbers(context: Context): List<String> {
        val blockedNumbers = mutableListOf<String>()

        if (!isDefaultSmsApp(context)) {
            showToast(getString(R.string.app_is_not_default_sms_app))
            return blockedNumbers
        }

        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)

        try {
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val numberIndex =
                    it.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                while (it.moveToNext()) {
                    val number = it.getString(numberIndex)
                    blockedNumbers.add(number)
                }
            }
        } catch (e: SecurityException) {
            showToast(getString(R.string.permission_denied_or_app_is_not_eligible, e.message))
        }

        return blockedNumbers
    }

    private fun blockNumber(context: Context, number: String): Boolean {
        if (!isDefaultSmsApp(context)) {
            showToast(getString(R.string.app_is_not_default_sms_app))
            return false
        }

        return try {
            val values = ContentValues().apply {
                put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            }
            val uri = context.contentResolver.insert(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI, values
            )
            uri != null
        } catch (_: Exception) {
            false
        }
    }

    private fun unblockNumber(context: Context, number: String): Boolean {
        if (!isDefaultSmsApp(context)) {
            showToast(getString(R.string.app_is_not_default_sms_app))
            return false
        }

        val uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
        val selectionArgs = arrayOf(number)

        return try {
            val rowsDeleted = context.contentResolver.delete(uri, selection, selectionArgs)
            rowsDeleted > 0
        } catch (_: Exception) {
            false
        }
    }

    override fun onItemClick(phoneNumber: String) {
        showBlockOrUnblockDialog(phoneNumber)
    }

    private fun showBlockOrUnblockDialog(phoneNumber: String) {
        val dialog = Dialog(this)
        val dialogBlockOrUnblockBinding: DialogBlockNumberBinding =
            DialogBlockNumberBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(dialogBlockOrUnblockBinding.root)

        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(R.color.transparent)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.setDimAmount(0.6f)

            val metrics = resources.displayMetrics
            window.attributes = window.attributes.apply {
                width = (metrics.widthPixels * 0.9f).toInt()
                height = WindowManager.LayoutParams.WRAP_CONTENT
            }
        }

        dialogBlockOrUnblockBinding.apply {
            if (phoneNumber != Const.STRING_DEFAULT_VALUE) {
                rvBlockNumber.visibility = View.GONE
                txtStatement.visibility = View.VISIBLE
                val titleTxt = resources.getString(R.string.unblock) + " " + phoneNumber + " ?"
                val subTitleTxt =
                    resources.getString(R.string.are_you_sure_you_want_to_unblock_this_number)
                txtTitle.text = titleTxt
                txtStatement.text = subTitleTxt
                btnYes.text = resources.getString(R.string.unblock)
            } else {
                txtStatement.visibility = View.GONE
                rvBlockNumber.visibility = View.VISIBLE
                val titleTxt = resources.getString(R.string.block_messages_from)
                btnYes.text = resources.getString(R.string.block)
                txtTitle.text = titleTxt
            }
        }

        dialogBlockOrUnblockBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBlockOrUnblockBinding.btnYes.setOnClickListener {
            dialog.dismiss()

            lifecycleScope.launch(Dispatchers.IO) {
                val isSuccess = if (phoneNumber != Const.STRING_DEFAULT_VALUE) {
                    unblockNumber(this@BlockedNumberActivity, phoneNumber)
                } else {
                    blockNumber(
                        this@BlockedNumberActivity,
                        dialogBlockOrUnblockBinding.etBlockContacts.text.toString().trim()
                    )
                }

                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        showToast(
                            getString(
                                if (phoneNumber != Const.STRING_DEFAULT_VALUE) R.string.contact_has_been_unblocked_successfully
                                else R.string.contact_has_been_blocked_successfully
                            )
                        )
                        getBlockedNumbersList()
                    }
                }
            }

            if (phoneNumber != Const.STRING_DEFAULT_VALUE) {
                if (unblockNumber(
                        this, phoneNumber
                    )
                ) showToast(getString(R.string.contact_has_been_unblocked_successfully))
            } else {
                if (blockNumber(
                        this, dialogBlockOrUnblockBinding.etBlockContacts.text.toString()
                    )
                ) showToast(getString(R.string.contact_has_been_blocked_successfully))
            }
            getBlockedNumbersList()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    override fun onResume() {
        if (!isDefaultSmsApp(this)) {
            startActivity(Intent(this, HomeActivity::class.java))
            finishAffinity()
        }
        super.onResume()
    }
}