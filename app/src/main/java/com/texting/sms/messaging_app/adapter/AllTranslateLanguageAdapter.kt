package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemLanguageBinding
import com.texting.sms.messaging_app.listener.SelectLanguageInterface
import com.texting.sms.messaging_app.response.Language
import com.texting.sms.messaging_app.utils.getColorFromAttr

class AllTranslateLanguageAdapter(
    private var languageList: MutableList<Language>,
    private var selectLanguageInterface: SelectLanguageInterface,
    private var context: Context
) : RecyclerView.Adapter<AllTranslateLanguageAdapter.ViewHolder>() {

    private var rowIndex = -1

    class ViewHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(languageList[position]) {
                binding.txtLanguage.text = language
            }

            itemView.setOnClickListener {
                rowIndex = bindingAdapterPosition
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < languageList.size) {
                    selectLanguageInterface.onItemClick(languageList[bindingAdapterPosition])
                    notifyDataSetChanged()
                }
            }

            if (rowIndex == bindingAdapterPosition) {
                binding.txtLanguage.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.app_theme_color
                    )
                )
            } else {
                binding.txtLanguage.setTextColor(context.getColorFromAttr(R.attr.subTextColor))
            }
        }
    }

    override fun getItemCount(): Int {
        return languageList.size
    }

    fun updateData(newList: List<Language>) {
        val diffCallback = AppLanguageDiffCallback(languageList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        languageList.clear()
        languageList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class AppLanguageDiffCallback(
    private val oldList: List<Language>, private val newList: List<Language>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].code == newList[newItemPosition].code
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}