package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemHorizontalQuickMessageBinding
import com.texting.sms.messaging_app.listener.OnQuickMessageClickInterface
import com.texting.sms.messaging_app.model.QuickResponse

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
            with(quickResponseMessageList[absoluteAdapterPosition]) {
                binding.txtQuickMessage.text = message
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < quickResponseMessageList.size) {
                    onQuickMessageInterface.onQuickMessageItemClick(
                        quickResponseMessageList[absoluteAdapterPosition]
                    )
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return quickResponseMessageList.size
    }
}