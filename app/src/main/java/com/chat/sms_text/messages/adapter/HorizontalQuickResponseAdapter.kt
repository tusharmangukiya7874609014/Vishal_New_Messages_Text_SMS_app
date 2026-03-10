package com.chat.sms_text.messages.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.databinding.ItemHorizontalQuickMessageBinding
import com.chat.sms_text.messages.listener.OnQuickMessageClickInterface
import com.chat.sms_text.messages.model.QuickResponse

class HorizontalQuickResponseAdapter(
    private var quickResponseMessageList: MutableList<QuickResponse>,
    private var onQuickMessageInterface: OnQuickMessageClickInterface
) : RecyclerView.Adapter<HorizontalQuickResponseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHorizontalQuickMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemHorizontalQuickMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(quickResponseMessageList[position]) {
                binding.txtQuickMessage.text = message
            }

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < quickResponseMessageList.size) {
                    onQuickMessageInterface.onQuickMessageItemClick(
                        quickResponseMessageList[bindingAdapterPosition]
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return quickResponseMessageList.size
    }
}