package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemTopContactsBinding
import com.texting.sms.messaging_app.listener.OnChatUserInterface
import com.texting.sms.messaging_app.model.ChatUser

class TopContactsAdapter(
    private var topMessageList: MutableList<ChatUser>,
    private var onChartUserInterface: OnChatUserInterface
) : RecyclerView.Adapter<TopContactsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemTopContactsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemTopContactsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.item = topMessageList[position]
            binding.executePendingBindings()

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < topMessageList.size) {
                    onChartUserInterface.chatUserClick(topMessageList[bindingAdapterPosition])
                }
            }
        }
    }

    fun updateData(newList: List<ChatUser>) {
        val diffCallback = TopContactsDiffCallback(topMessageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        topMessageList.clear()
        topMessageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return if (topMessageList.size > 10) 10 else (topMessageList.size)
    }
}

class TopContactsDiffCallback(
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