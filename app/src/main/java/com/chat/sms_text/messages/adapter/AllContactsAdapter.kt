package com.chat.sms_text.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.databinding.ItemContactBinding
import com.chat.sms_text.messages.databinding.ItemHeaderWithFirstContactBinding
import com.chat.sms_text.messages.listener.OnClickContactInterface
import com.chat.sms_text.messages.model.ContactModel
import com.l4digital.fastscroll.FastScroller

class AllContactsAdapter(
    private var callHistoryList: MutableList<ContactModel>,
    private var onContactInterface: OnClickContactInterface,
    private var isVisibleNumber: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    FastScroller.SectionIndexer {

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
                val binding = ItemHeaderWithFirstContactBinding.inflate(
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

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        onBindViewHolder(holder, position)
    }

    override fun getSectionText(position: Int): CharSequence {
        if (callHistoryList.isEmpty()) return ""

        return when (val item = callHistoryList[position]) {
            is ContactModel.HeaderWithFirstItem -> {
                item.title
            }

            is ContactModel.ContactItem -> {
                item.name?.firstOrNull()?.uppercase() ?: "#"
            }
        }
    }

    inner class HeaderWithFirstViewHolder(
        private val binding: ItemHeaderWithFirstContactBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contactInfo: ContactModel.HeaderWithFirstItem) {
            binding.isContactsVisible = isVisibleNumber
            binding.item = contactInfo
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onContactInterface.onItemClick(contactInfo.firstContact)
                }
            }
        }
    }

    inner class LogViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contactInfo: ContactModel.ContactItem) {
            binding.isContactsVisible = isVisibleNumber
            binding.item = contactInfo
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onContactInterface.onItemClick(contactInfo)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return callHistoryList.size
    }

    fun updateData(newList: List<ContactModel>) {
        val diffCallback = AllContactDiffCallback(callHistoryList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        callHistoryList.clear()
        callHistoryList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class AllContactDiffCallback(
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