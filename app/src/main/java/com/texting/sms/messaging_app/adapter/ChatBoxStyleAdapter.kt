package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ItemChatBoxStyleBinding
import com.texting.sms.messaging_app.listener.OnChatBoxClickInterface
import com.texting.sms.messaging_app.model.ChatBoxStyle

class ChatBoxStyleAdapter(
    private var chatBoxStyleList: MutableList<ChatBoxStyle>,
    private var onChatBoxClickInterface: OnChatBoxClickInterface,
    context: Context
) : RecyclerView.Adapter<ChatBoxStyleAdapter.ViewHolder>() {

    private var selectedPosition =
        SharedPreferencesHelper.getInt(context, Const.CHAT_BOX_STYLE_POSITION, 0)
    private var lastSelectedPosition = 0

    class ViewHolder(val binding: ItemChatBoxStyleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemChatBoxStyleBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.executePendingBindings()

            if (selectedPosition == absoluteAdapterPosition) {
                chatBoxStyleList[absoluteAdapterPosition].isChatBoxSelected = true
                lastSelectedPosition = absoluteAdapterPosition
            } else {
                chatBoxStyleList[position].isChatBoxSelected = false
            }

            binding.item = chatBoxStyleList[absoluteAdapterPosition]

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < chatBoxStyleList.size) {
                    selectedPosition = absoluteAdapterPosition

                    onChatBoxClickInterface.onSelectChatBoxClick(
                        absoluteAdapterPosition,
                        chatBoxStyleList[absoluteAdapterPosition]
                    )
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
            holder.binding.item = chatBoxStyleList[holder.absoluteAdapterPosition]
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun updateSelectedChatBoxStyle(selectedPosition: Int) {
        /**  Update last selected position  **/
        chatBoxStyleList[lastSelectedPosition].isChatBoxSelected = false
        notifyItemChanged(lastSelectedPosition, "payload_update")

        /**  Update new selected position  **/
        lastSelectedPosition = selectedPosition
        chatBoxStyleList[selectedPosition].isChatBoxSelected = true
        notifyItemChanged(selectedPosition, "payload_update")
    }

    override fun getItemCount(): Int {
        return chatBoxStyleList.size
    }

    fun updateData(newList: List<ChatBoxStyle>) {
        val diffCallback = ChatBoxStyleDiffCallback(chatBoxStyleList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        chatBoxStyleList.clear()
        chatBoxStyleList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class ChatBoxStyleDiffCallback(
    private val oldList: List<ChatBoxStyle>, private val newList: List<ChatBoxStyle>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}