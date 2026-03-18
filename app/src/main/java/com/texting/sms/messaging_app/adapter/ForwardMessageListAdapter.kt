package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.texting.sms.messaging_app.databinding.ItemSelectedForwardMessageBinding
import com.texting.sms.messaging_app.listener.OnSelectedMessageRemove

class ForwardMessageListAdapter(
    private var selectedMessageList: MutableList<String>,
    private var onRemoveSelectedMessage: OnSelectedMessageRemove,
    private var context: Context
) : RecyclerView.Adapter<ForwardMessageListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectedForwardMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSelectedForwardMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(selectedMessageList[absoluteAdapterPosition]) {
                val messageId = selectedMessageList[absoluteAdapterPosition]
                if (isValidMessageId(messageId)) {
                    binding.rvSelectedImages.visibility = View.GONE
                    val messageOfSMS = getSmsBodyById(context = context, messageId = messageId)
                    binding.txtSelectedText.text = messageOfSMS
                    binding.rvSelectedText.visibility = View.VISIBLE
                } else {
                    binding.rvSelectedText.visibility = View.GONE
                    val regex = Regex("^\\d+")
                    val messageIdForImage = regex.find(this)?.value ?: ""

                    val photoUri = "content://mms/part/$messageIdForImage".toUri()
                    Glide.with(context)
                        .load(photoUri)
                        .into(binding.ivSelectedImages)

                    binding.rvSelectedImages.visibility = View.VISIBLE
                }

                binding.ivRemoveSMS.setOnClickListener {
                    if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < selectedMessageList.size) {
                        onRemoveSelectedMessage.onRemoveSelectedMessage(
                            selectedMessageList[absoluteAdapterPosition],
                            absoluteAdapterPosition
                        )
                    }
                }

                binding.ivSelectedImages.setOnClickListener {
                    if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < selectedMessageList.size) {
                        onRemoveSelectedMessage.onRemoveSelectedMessage(
                            selectedMessageList[absoluteAdapterPosition],
                            absoluteAdapterPosition
                        )
                    }
                }
            }
        }
    }

    private fun getSmsBodyById(context: Context, messageId: String): String? {
        val uri = Uri.withAppendedPath("content://sms".toUri(), messageId)
        val projection = arrayOf("body")

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("body"))
            }
        }
        return null
    }

    private fun isValidMessageId(messageId: String): Boolean {
        return !messageId.contains("(Images)")
    }

    fun updateData(newList: List<String>) {
        val diffCallback = ForwardMessageDiffCallback(selectedMessageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        selectedMessageList.clear()
        selectedMessageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return selectedMessageList.size
    }
}

class ForwardMessageDiffCallback(
    private val oldList: List<String>, private val newList: List<String>
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