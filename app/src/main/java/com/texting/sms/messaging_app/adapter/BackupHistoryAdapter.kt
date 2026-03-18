package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemBackupHistoryBinding
import com.texting.sms.messaging_app.listener.OnBackupClickInterface
import com.texting.sms.messaging_app.model.SmsBackupInfo

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
            with(backupHistory[absoluteAdapterPosition]) {
                binding.txtBackUpDate.text = backupTime
                binding.txtBackUpSize.text = fileSize
                val totalMessageTxt =
                    totalMessages.toString() + " " + context.resources.getString(R.string.messages)
                binding.txtBackUpTotalMessage.text = totalMessageTxt
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < backupHistory.size) {
                    onBackupClickInterface.onSelectBackupClick(backupHistory[absoluteAdapterPosition])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return backupHistory.size
    }
}