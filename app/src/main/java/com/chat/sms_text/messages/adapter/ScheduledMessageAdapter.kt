package com.chat.sms_text.messages.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.databinding.ItemScheduledMessageBinding
import com.chat.sms_text.messages.listener.OnScheduledClickInterface
import com.chat.sms_text.messages.model.ScheduledSms

class ScheduledMessageAdapter(
    private var scheduledMessageList: MutableList<ScheduledSms>,
    private var onScheduledClickInterface: OnScheduledClickInterface,
    private var context: Context
) : RecyclerView.Adapter<ScheduledMessageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemScheduledMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemScheduledMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.apply {
                item = scheduledMessageList[position]
                executePendingBindings()
            }

            if (scheduledMessageList[position].imageURIs.isNotEmpty()) {
                val selectedImageUris: MutableList<Uri> =
                    scheduledMessageList[position].imageURIs.split(",").map { it.toUri() }
                        .toMutableList()

                binding.rvSelectedImages.layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                val chatHistoryImageAdapter =
                    ChatHistoryImageAdapter(selectedImageUris, context) { _ ->
                        if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < scheduledMessageList.size) {
                            onScheduledClickInterface.onItemClick(scheduledMessageList[bindingAdapterPosition])
                        }
                    }
                binding.rvSelectedImages.adapter = chatHistoryImageAdapter
                binding.rvSelectedImages.visibility = View.VISIBLE
            } else {
                binding.rvSelectedImages.visibility = View.GONE
            }

            binding.rvMainContentView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < scheduledMessageList.size) {
                    onScheduledClickInterface.onItemClick(scheduledMessageList[bindingAdapterPosition])
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return scheduledMessageList.size
    }

    fun updateData(newList: MutableList<ScheduledSms>) {
        val diffCallback = ScheduledMessageDiffCallback(scheduledMessageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        scheduledMessageList.clear()
        scheduledMessageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class ScheduledMessageDiffCallback(
    private val oldList: List<ScheduledSms>, private val newList: List<ScheduledSms>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}