package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.l4digital.fastscroll.FastScroller
import com.texting.sms.messaging_app.databinding.ItemContactBinding
import com.texting.sms.messaging_app.databinding.ItemHeaderWithFirstContactBinding
import com.texting.sms.messaging_app.diffutils.AllContactDiffCallback
import com.texting.sms.messaging_app.listener.OnClickContactInterface
import com.texting.sms.messaging_app.model.ContactModel

class AllContactsAdapter(
    private var onContactInterface: OnClickContactInterface,
    private var isVisibleNumber: Boolean = true
) : ListAdapter<ContactModel, RecyclerView.ViewHolder>(
    AllContactDiffCallback
), FastScroller.SectionIndexer {

    init {
        setHasStableIds(true)
    }

    companion object {
        const val VIEW_TYPE_HEADER_WITH_FIRST = 0
        const val VIEW_TYPE_CONTACT_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ContactModel.HeaderWithFirstItem -> VIEW_TYPE_HEADER_WITH_FIRST
            is ContactModel.ContactItem -> VIEW_TYPE_CONTACT_ITEM
        }
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is ContactModel.ContactItem -> item.contactId.hashCode().toLong()

            is ContactModel.HeaderWithFirstItem -> item.firstContact.contactId.hashCode().toLong()
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
                ContactViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ContactModel.HeaderWithFirstItem -> (holder as HeaderWithFirstViewHolder).bind(item)
            is ContactModel.ContactItem -> (holder as ContactViewHolder).bind(item)
        }
    }

    override fun getSectionText(position: Int): CharSequence {
        if (currentList.isEmpty()) return ""

        return when (val item = getItem(position)) {
            is ContactModel.HeaderWithFirstItem -> {
                item.firstContact.name?.firstOrNull()?.uppercase() ?: "#"
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
                if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                onContactInterface.onItemClick(contactInfo.firstContact)
            }
        }
    }

    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contactInfo: ContactModel.ContactItem) {
            binding.isContactsVisible = isVisibleNumber
            binding.item = contactInfo
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                if (bindingAdapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                onContactInterface.onItemClick(contactInfo)
            }
        }
    }
}