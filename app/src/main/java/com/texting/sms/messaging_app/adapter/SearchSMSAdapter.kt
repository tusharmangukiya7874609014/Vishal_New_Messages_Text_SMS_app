package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemSmsSearchBinding
import com.texting.sms.messaging_app.listener.OnSearchResultClickInterface
import com.texting.sms.messaging_app.model.ChatMatchResult

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
            with(messageFilterList[absoluteAdapterPosition]) {
                binding.item = messageFilterList[absoluteAdapterPosition]
                binding.executePendingBindings()

                val finalCountMatches = "$matchCount Messages"
                binding.txtLastMessage.text = finalCountMatches

                itemView.setOnClickListener {
                    if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < messageFilterList.size) {
                        val currentMessage = messageFilterList.getOrNull(absoluteAdapterPosition)
                        if (currentMessage != null) {
                            onSearchResultClickInterface.onItemClick(messageFilterList[absoluteAdapterPosition])
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
                        "${messageFilterList[absoluteAdapterPosition].matchCount} Messages"
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