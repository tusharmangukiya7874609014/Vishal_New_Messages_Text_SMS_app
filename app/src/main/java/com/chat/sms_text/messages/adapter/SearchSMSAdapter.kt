package com.chat.sms_text.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.databinding.ItemSmsSearchBinding
import com.chat.sms_text.messages.listener.OnSearchResultClickInterface
import com.chat.sms_text.messages.model.ChatMatchResult

class SearchSMSAdapter(
    private var messageFilterList: MutableList<ChatMatchResult>,
    private var onSearchResultClickInterface: OnSearchResultClickInterface
) : RecyclerView.Adapter<SearchSMSAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSmsSearchBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSmsSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(messageFilterList[position]) {
                binding.item = messageFilterList[position]
                binding.executePendingBindings()

                val finalCountMatches = "$matchCount Messages"
                binding.txtLastMessage.text = finalCountMatches

                itemView.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < messageFilterList.size) {
                        val currentMessage = messageFilterList.getOrNull(bindingAdapterPosition)
                        if (currentMessage != null) {
                            onSearchResultClickInterface.onItemClick(messageFilterList[bindingAdapterPosition])
                        }
                    }
                }
            }
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
                    val finalCountMatches =
                        "${messageFilterList[position].matchCount} Messages"
                    binding.txtLastMessage.text = finalCountMatches
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateData(newList: List<ChatMatchResult>) {
        val diffCallback = SearchSMSDiffCallback(messageFilterList, newList)
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

class SearchSMSDiffCallback(
    private val oldList: List<ChatMatchResult>, private val newList: List<ChatMatchResult>
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