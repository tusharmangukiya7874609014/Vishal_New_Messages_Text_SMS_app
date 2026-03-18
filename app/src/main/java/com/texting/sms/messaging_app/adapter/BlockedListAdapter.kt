package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemBlockedUserBinding
import com.texting.sms.messaging_app.listener.UnblockUserInterface

class BlockedListAdapter(
    private var blockedList: List<String>,
    private var unblockUserInterface: UnblockUserInterface
) : RecyclerView.Adapter<BlockedListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemBlockedUserBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemBlockedUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(blockedList[absoluteAdapterPosition]) {
                binding.txtContactNumber.text = this
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < blockedList.size) {
                    unblockUserInterface.onItemClick(blockedList[absoluteAdapterPosition])
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun updateBlockedList(newBlockedList: List<String>) {
        blockedList = newBlockedList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return blockedList.size
    }
}