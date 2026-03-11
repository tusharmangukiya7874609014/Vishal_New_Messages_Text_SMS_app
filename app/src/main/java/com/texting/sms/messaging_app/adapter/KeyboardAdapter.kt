package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemCustomKeyboardBinding
import com.texting.sms.messaging_app.listener.OnClickKeyboardInterface
import com.texting.sms.messaging_app.model.Password

class KeyboardAdapter(
    private var numberList: MutableList<Password>,
    private var keyboardInterface: OnClickKeyboardInterface
) : RecyclerView.Adapter<KeyboardAdapter.ViewHolder>() {

    private var rowIndex = -1
    private var lastSelectedPosition = 0

    class ViewHolder(val binding: ItemCustomKeyboardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCustomKeyboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.item = numberList[position]
            binding.executePendingBindings()

            if (rowIndex == position) {
                numberList[position].isNumberSelected = true
                lastSelectedPosition = position
            } else {
                numberList[position].isNumberSelected = false
            }

            binding.rlNumberView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < numberList.size) {
                    rowIndex = bindingAdapterPosition
                    keyboardInterface.onItemClick(
                        numberList[bindingAdapterPosition],
                        position
                    )
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            holder.binding.item = numberList[position]
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun updateSelectedNumber(selectedPosition: Int) {
        /**  Update last selected position  **/
        numberList[lastSelectedPosition].isNumberSelected = false
        notifyItemChanged(lastSelectedPosition, "payload_update")

        /**  Update new selected position  **/
        lastSelectedPosition = selectedPosition
        numberList[selectedPosition].isNumberSelected = true
        notifyItemChanged(selectedPosition, "payload_update")
    }

    override fun getItemCount(): Int {
        return numberList.size
    }

    fun onClearView() {
        numberList.forEachIndexed { index, password ->
            password.isNumberSelected = false
            notifyItemChanged(index, "payload_update")
        }
    }

    fun updateData(newList: List<Password>) {
        val diffCallback = KeyboardDiffCallback(numberList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        numberList.clear()
        numberList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class KeyboardDiffCallback(
    private val oldList: List<Password>, private val newList: List<Password>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}