package com.chat.sms_text.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.databinding.ItemMessageHistoryBinding
import com.chat.sms_text.messages.listener.OnChatUserInterface
import com.chat.sms_text.messages.model.ChatUser

class SearchContactsAdapter(
    private var messageFilterList: MutableList<ChatUser>,
    private var onChartUserInterface: OnChatUserInterface
) : RecyclerView.Adapter<SearchContactsAdapter.ViewHolder>() {

    private var searchQuery: String = ""

    class ViewHolder(val binding: ItemMessageHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemMessageHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.executePendingBindings()

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < messageFilterList.size) {
                    onChartUserInterface.chatUserClick(messageFilterList[bindingAdapterPosition])
                }
            }

            binding.searchQuery = searchQuery
            binding.item = messageFilterList[position]
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0]
            if (position >= messageFilterList.size) return
            with(holder) {
                if (payload == "payload_update") {
                    binding.searchQuery = searchQuery
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateData(newList: List<ChatUser>, query: String) {
        searchQuery = query
        val diffCallback = SearchContactsDiffCallback(messageFilterList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messageFilterList.clear()
        messageFilterList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
        messageFilterList.forEachIndexed { index, _ ->
            notifyItemChanged(index, "payload_update")
        }
    }

    override fun getItemCount(): Int {
        return messageFilterList.size
    }
}

class SearchContactsDiffCallback(
    private val oldList: List<ChatUser>, private val newList: List<ChatUser>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].threadId == newList[newItemPosition].threadId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}