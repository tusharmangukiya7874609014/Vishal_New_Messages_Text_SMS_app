package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemSuggestedClipBoardItemBinding
import com.texting.sms.messaging_app.listener.OnClipboardClickInterface

class ClipboardListAdapter(
    private var clipBoardMessageList: MutableList<String>,
    private var onClipboardClickInterface: OnClipboardClickInterface
) : RecyclerView.Adapter<ClipboardListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSuggestedClipBoardItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSuggestedClipBoardItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(clipBoardMessageList[absoluteAdapterPosition]) {
                binding.txtClipBoard.text = this
            }

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < clipBoardMessageList.size) {
                    onClipboardClickInterface.onItemClick(clipBoardMessageList[absoluteAdapterPosition])
                }
            }
        }
    }

    fun updateData(newList: MutableList<String>) {
        val diffCallback = ClipboardDiffCallback(clipBoardMessageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        clipBoardMessageList.clear()
        clipBoardMessageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return clipBoardMessageList.size
    }
}

class ClipboardDiffCallback(
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