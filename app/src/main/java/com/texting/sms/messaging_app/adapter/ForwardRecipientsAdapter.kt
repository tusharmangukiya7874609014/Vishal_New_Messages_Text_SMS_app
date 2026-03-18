package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemSelectRecipientsBinding
import com.texting.sms.messaging_app.listener.OnRemoveForwardRecipients
import com.texting.sms.messaging_app.model.ContactModel

class ForwardRecipientsAdapter(
    private var selectedContactsList: MutableList<ContactModel.ContactItem>,
    private var onRemoveForwardRecipients: OnRemoveForwardRecipients
) : RecyclerView.Adapter<ForwardRecipientsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectRecipientsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSelectRecipientsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.item = selectedContactsList[absoluteAdapterPosition]
            binding.executePendingBindings()

            binding.ivRemoveImage.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < selectedContactsList.size) {
                    onRemoveForwardRecipients.onItemRemoved(
                        selectedContactsList[absoluteAdapterPosition],
                        absoluteAdapterPosition
                    )
                }
            }
        }
    }

    fun updateData(newList: List<ContactModel.ContactItem>) {
        val diffCallback = ForwardRecipientsDiffCallback(selectedContactsList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        selectedContactsList.clear()
        selectedContactsList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return selectedContactsList.size
    }
}

class ForwardRecipientsDiffCallback(
    private val oldList: List<ContactModel.ContactItem>,
    private val newList: List<ContactModel.ContactItem>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].contactId == newList[newItemPosition].contactId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}