package com.chat.sms_text.messages.adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.activity.AllConversationsActivity
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ItemMessageHistoryBinding
import com.chat.sms_text.messages.listener.OnChatUserInterface
import com.chat.sms_text.messages.model.ChatUser

class AllConversationAdapter(
    private var messageFilterList: MutableList<ChatUser>,
    private var selectedThreadIDList: ArrayList<String>,
    private var onChartUserInterface: OnChatUserInterface,
    private var activeSIM: Int = 0,
    private var context: Context
) : RecyclerView.Adapter<AllConversationAdapter.ViewHolder>() {

    private var isLongClicked = false
    private var isMultiSelectionEnableFromPage = true

    class ViewHolder(val binding: ItemMessageHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemMessageHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                isMultiSelectionOptionsEnable = isMultiSelectionEnableFromPage
                activateSimDetails = activeSIM

                executePendingBindings()
            }

            val threadId = messageFilterList[position].threadId.toString()
            messageFilterList[position].isMessageSelected =
                selectedThreadIDList.contains(threadId)

            binding.item = messageFilterList[position]

            itemView.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION || bindingAdapterPosition >= messageFilterList.size) return@setOnClickListener

                val threadId = messageFilterList[bindingAdapterPosition].threadId.toString()

                if (isMultiSelectionEnableFromPage) {
                    if (selectedThreadIDList.contains(threadId)) {
                        selectedThreadIDList.remove(threadId)
                        messageFilterList[bindingAdapterPosition].isMessageSelected = false
                    } else {
                        selectedThreadIDList.add(threadId)
                        messageFilterList[bindingAdapterPosition].isMessageSelected = true
                    }

                    binding.item = messageFilterList[bindingAdapterPosition]
                    SharedPreferencesHelper.saveArrayList(
                        context, Const.PRIVATE_MESSAGE_IDS, selectedThreadIDList
                    )
                    (context as AllConversationsActivity).updateSelectedCount(
                        selectedThreadIDList.size
                    )
                } else {
                    onChartUserInterface.chatUserClick(messageFilterList[bindingAdapterPosition])
                }
            }

            binding.rvCopyCode.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION || bindingAdapterPosition >= messageFilterList.size) return@setOnClickListener

                val detectedOtp = binding.rvCopyCode.tag as? String ?: return@setOnClickListener

                if (detectedOtp.isNotEmpty()) {
                    copyToClipboard(context, detectedOtp)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            val payload = payloads[0]
            if (position >= messageFilterList.size) return
            with(holder) {
                if (payload == "partialClear") {
                    messageFilterList[position].isMessageSelected = false
                    binding.item = messageFilterList[position]
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun updateItemByThreadId(threadId: Long, chatUser: ChatUser?) {
        val position = messageFilterList.indexOfFirst { it.threadId == threadId }
        if (position != -1) {
            if (chatUser != null) {
                messageFilterList[position] = chatUser
            }
            notifyItemChanged(position)
        }
    }

    fun clearAndUpdateView() {
        selectedThreadIDList.clear()
        isLongClicked = false
        for (i in 0..messageFilterList.size) {
            notifyItemChanged(i, "partialClear")
        }
    }

    private fun copyToClipboard(context: Context, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OTP", code)
        clipboard.setPrimaryClip(clip)
    }

    fun updateData(newList: List<ChatUser>) {
        val diffCallback = AllConversationsChatDiffCallback(messageFilterList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messageFilterList.clear()
        messageFilterList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return messageFilterList.size
    }
}

class AllConversationsChatDiffCallback(
    private val oldList: List<ChatUser>, private val newList: List<ChatUser>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].threadId == newList[newItemPosition].threadId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem.unreadCount == newItem.unreadCount && oldItem.latestMessage == newItem.latestMessage && oldItem.timestamp == newItem.timestamp && oldItem.address == newItem.address && oldItem.isPinned == newItem.isPinned && oldItem.simSlot == newItem.simSlot && oldItem.photoUri == newItem.photoUri
    }
}