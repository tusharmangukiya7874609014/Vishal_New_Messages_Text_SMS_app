package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ItemLanguageListBinding
import com.texting.sms.messaging_app.listener.LanguageInterface
import com.texting.sms.messaging_app.model.AppLanguage

class LanguageAdapter(
    private var languageList: MutableList<AppLanguage>,
    private var languageInterface: LanguageInterface,
    context: Context
) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    private var rowIndex = SharedPreferencesHelper.getLanguage(context)
    private var lastSelectedPosition = 0

    class ViewHolder(val binding: ItemLanguageListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemLanguageListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.executePendingBindings()

            if (rowIndex == languageList[absoluteAdapterPosition].languageCode) {
                languageList[absoluteAdapterPosition].isLanguageSelected = true
                lastSelectedPosition = absoluteAdapterPosition
            } else {
                languageList[absoluteAdapterPosition].isLanguageSelected = false
            }

            binding.item = languageList[absoluteAdapterPosition]

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < languageList.size) {
                    rowIndex = languageList[absoluteAdapterPosition].languageCode.toString()

                    languageInterface.onItemClick(
                        languageList[absoluteAdapterPosition], absoluteAdapterPosition
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
            holder.binding.item = languageList[holder.absoluteAdapterPosition]
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun updateSelectedLanguage(selectedPosition: Int) {
        /**  Update last selected position  **/
        languageList[lastSelectedPosition].isLanguageSelected = false
        notifyItemChanged(lastSelectedPosition, "payload_update")

        /**  Update new selected position  **/
        lastSelectedPosition = selectedPosition
        languageList[selectedPosition].isLanguageSelected = true
        notifyItemChanged(selectedPosition, "payload_update")
    }

    override fun getItemCount(): Int {
        return languageList.size
    }

    fun updateData(newList: List<AppLanguage>) {
        val diffCallback = LanguageDiffCallback(languageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        languageList.clear()
        languageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class LanguageDiffCallback(
    private val oldList: List<AppLanguage>, private val newList: List<AppLanguage>
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