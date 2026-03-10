package com.chat.sms_text.messages.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.activity.SelectRecipientsActivity
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.databinding.ItemContactBinding
import com.chat.sms_text.messages.databinding.ItemHeaderWithFirstContactBinding
import com.chat.sms_text.messages.model.ContactModel

class SelectRecipientsAdapter(
    private var callHistoryList: MutableList<ContactModel>,
    private var selectedMobileList: ArrayList<String>,
    private var context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    companion object {
        const val VIEW_TYPE_HEADER_WITH_FIRST = 0
        const val VIEW_TYPE_LOG = 1
    }

    override fun getItemId(position: Int): Long {
        return when (val item = callHistoryList[position]) {
            is ContactModel.ContactItem ->
                item.contactId.hashCode().toLong()

            is ContactModel.HeaderWithFirstItem ->
                item.title.hashCode().toLong()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (callHistoryList[position]) {
            is ContactModel.HeaderWithFirstItem -> VIEW_TYPE_HEADER_WITH_FIRST
            is ContactModel.ContactItem -> VIEW_TYPE_LOG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER_WITH_FIRST -> {
                val binding =
                    ItemHeaderWithFirstContactBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                HeaderWithFirstViewHolder(binding)
            }

            else -> {
                val binding = ItemContactBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                LogViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = callHistoryList[position]) {
            is ContactModel.HeaderWithFirstItem -> (holder as HeaderWithFirstViewHolder).bind(item)
            is ContactModel.ContactItem -> (holder as LogViewHolder).bind(item)
        }
    }

    inner class HeaderWithFirstViewHolder(
        private val binding: ItemHeaderWithFirstContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contactInfo: ContactModel.HeaderWithFirstItem) {
            with(contactInfo.firstContact) {
                binding.executePendingBindings()

                binding.root.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        if (selectedMobileList.contains(phoneNumbers)) {
                            selectedMobileList.remove(phoneNumbers)
                            contactInfo.firstContact.isSelectedProfile = false
                        } else {
                            contactInfo.firstContact.isSelectedProfile = true
                            selectedMobileList.add(phoneNumbers.toString())
                        }

                        binding.item = contactInfo

                        SharedPreferencesHelper.saveArrayList(
                            context,
                            Const.SELECTED_MOBILE_LIST,
                            selectedMobileList
                        )
                        (context as SelectRecipientsActivity).updateSelectedCount(selectedMobileList.size)
                    }
                }

                contactInfo.firstContact.isSelectedProfile =
                    selectedMobileList.contains(phoneNumbers)

                binding.item = contactInfo
            }
        }

        fun clearAndUpdateView(item: ContactModel.HeaderWithFirstItem) {
            item.firstContact.isSelectedProfile = false
            binding.item = item
        }
    }

    inner class LogViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contactInfo: ContactModel.ContactItem) {
            with(contactInfo) {
                binding.executePendingBindings()

                contactInfo.isSelectedProfile = selectedMobileList.contains(phoneNumbers)
                binding.item = contactInfo

                binding.root.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        if (selectedMobileList.contains(phoneNumbers)) {
                            selectedMobileList.remove(phoneNumbers.toString())
                            contactInfo.isSelectedProfile = false
                        } else {
                            selectedMobileList.add(phoneNumbers.toString())
                            contactInfo.isSelectedProfile = true
                        }

                        binding.item = contactInfo

                        SharedPreferencesHelper.saveArrayList(
                            context,
                            Const.SELECTED_MOBILE_LIST,
                            selectedMobileList
                        )
                        (context as SelectRecipientsActivity).updateSelectedCount(selectedMobileList.size)
                    }
                }
            }
        }

        fun clearAndUpdateView(item: ContactModel.ContactItem) {
            item.isSelectedProfile = false
            binding.item = item
        }
    }

    override fun getItemCount(): Int {
        return callHistoryList.size
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            when (val item = callHistoryList[position]) {
                is ContactModel.ContactItem -> {
                    if (holder is LogViewHolder && payloads.contains("partialClear")) {
                        holder.clearAndUpdateView(item)
                    }
                }

                is ContactModel.HeaderWithFirstItem -> {
                    if (holder is HeaderWithFirstViewHolder && payloads.contains("partialClear")) {
                        holder.clearAndUpdateView(item)
                    }
                }
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    fun specificClearAndUpdateView(phoneNumber: String) {
        callHistoryList.forEachIndexed { index, item ->
            if (item is ContactModel.ContactItem && item.phoneNumbers.toString()
                    .contains(phoneNumber)
            ) {
                selectedMobileList.remove(phoneNumber)
                notifyItemChanged(index, "partialClear")
            }
            if (item is ContactModel.HeaderWithFirstItem && item.firstContact.phoneNumbers.toString()
                    .contains(
                        phoneNumber
                    )
            ) {
                selectedMobileList.remove(phoneNumber)
                notifyItemChanged(index, "partialClear")
            }
        }
    }

    fun clearAndUpdateView() {
        selectedMobileList.clear()
        for (i in 0..callHistoryList.size) {
            notifyItemChanged(i, "partialClear")
        }
    }

    fun updateData(newList: List<ContactModel>) {
        val diffCallback = SelectContactDiffCallback(callHistoryList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        callHistoryList.clear()
        callHistoryList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class SelectContactDiffCallback(
    private val oldList: List<ContactModel>, private val newList: List<ContactModel>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return when (old) {
            is ContactModel.ContactItem if new is ContactModel.ContactItem -> {
                old.contactId == new.contactId
            }

            is ContactModel.HeaderWithFirstItem if new is ContactModel.HeaderWithFirstItem -> {
                old.title == new.title
            }

            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}