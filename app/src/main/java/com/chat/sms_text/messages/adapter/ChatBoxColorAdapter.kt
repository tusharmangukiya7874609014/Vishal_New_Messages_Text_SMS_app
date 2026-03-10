package com.chat.sms_text.messages.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ItemChatBoxColorBinding
import com.chat.sms_text.messages.listener.OnChatBoxColorClickInterface
import com.chat.sms_text.messages.model.ChatBoxColor

class ChatBoxColorAdapter(
    private var chatBoxColorList: MutableList<ChatBoxColor>,
    private var onChatBoxColorClickInterface: OnChatBoxColorClickInterface,
    context: Context
) : RecyclerView.Adapter<ChatBoxColorAdapter.ViewHolder>() {

    private var selectedPosition =
        SharedPreferencesHelper.getInt(context, Const.CHAT_BOX_COLOR_POSITION, 0)
    private var lastSelectedPosition = 0

    class ViewHolder(val binding: ItemChatBoxColorBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemChatBoxColorBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.executePendingBindings()

            if (selectedPosition == bindingAdapterPosition) {
                chatBoxColorList[position].isChatBoxColorSelected = true
                lastSelectedPosition = position
            } else {
                chatBoxColorList[position].isChatBoxColorSelected = false
            }

            binding.item = chatBoxColorList[position]

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < chatBoxColorList.size) {
                    selectedPosition = bindingAdapterPosition

                    onChatBoxColorClickInterface.onSelectChatBoxColorClick(
                        bindingAdapterPosition,
                        chatBoxColorList[bindingAdapterPosition]
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
            holder.binding.item = chatBoxColorList[position]
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun updateSelectedChatBoxColor(selectedPosition: Int) {
        /**  Update last selected position  **/
        chatBoxColorList[lastSelectedPosition].isChatBoxColorSelected = false
        notifyItemChanged(lastSelectedPosition, "payload_update")

        /**  Update new selected position  **/
        lastSelectedPosition = selectedPosition
        chatBoxColorList[selectedPosition].isChatBoxColorSelected = true
        notifyItemChanged(selectedPosition, "payload_update")
    }

    override fun getItemCount(): Int {
        return chatBoxColorList.size
    }

    fun updateData(newList: List<ChatBoxColor>) {
        val diffCallback = ChatBoxColorDiffCallback(chatBoxColorList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        chatBoxColorList.clear()
        chatBoxColorList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class ChatBoxColorDiffCallback(
    private val oldList: List<ChatBoxColor>, private val newList: List<ChatBoxColor>
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