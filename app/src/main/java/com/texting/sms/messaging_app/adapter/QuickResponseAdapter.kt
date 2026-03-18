package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemQuickResponseBinding
import com.texting.sms.messaging_app.listener.OnQuickMessageInterface
import com.texting.sms.messaging_app.model.QuickResponse

class QuickResponseAdapter(
    private var context: Context,
    private var quickResponseMessageList: MutableList<QuickResponse>,
    private var onQuickMessageInterface: OnQuickMessageInterface
) : RecyclerView.Adapter<QuickResponseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemQuickResponseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemQuickResponseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(quickResponseMessageList[absoluteAdapterPosition]) {
                binding.txtQuickMessage.text = message
            }

            binding.ivEditView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < quickResponseMessageList.size) {
                    onQuickMessageInterface.onQuickMessageEditClick(
                        quickResponseMessageList[absoluteAdapterPosition],
                        absoluteAdapterPosition
                    )
                }
            }

            binding.ivDeleteView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < quickResponseMessageList.size && absoluteAdapterPosition > 9) {
                    onQuickMessageInterface.onQuickMessageDeleteClick(
                        quickResponseMessageList[absoluteAdapterPosition],
                        absoluteAdapterPosition
                    )
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.this_message_cannot_be_deleted_because_it_is_a_default_quick_response),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return quickResponseMessageList.size
    }

    fun updateList(newList: MutableList<QuickResponse>) {
        quickResponseMessageList.clear()
        quickResponseMessageList.addAll(newList)
        notifyDataSetChanged()
    }
}