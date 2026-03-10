package com.chat.sms_text.messages.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.ItemBackupHistoryBinding
import com.chat.sms_text.messages.listener.OnBackupClickInterface
import com.chat.sms_text.messages.model.SmsBackupInfo

class BackupHistoryAdapter(
    private var backupHistory: List<SmsBackupInfo>,
    private var onBackupClickInterface: OnBackupClickInterface,
    private var context: Context
) : RecyclerView.Adapter<BackupHistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBackupHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemBackupHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(backupHistory[position]) {
                binding.txtBackUpDate.text = backupTime
                binding.txtBackUpSize.text = fileSize
                val totalMessageTxt =
                    totalMessages.toString() + " " + context.resources.getString(R.string.messages)
                binding.txtBackUpTotalMessage.text = totalMessageTxt
            }

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < backupHistory.size) {
                    onBackupClickInterface.onSelectBackupClick(backupHistory[bindingAdapterPosition])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return backupHistory.size
    }
}