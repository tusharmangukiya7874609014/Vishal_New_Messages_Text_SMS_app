package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemCustomMonthPickerBinding
import com.texting.sms.messaging_app.listener.YearInterface
import com.texting.sms.messaging_app.utils.getColorFromAttr
import java.time.LocalDate

class CustomYearPickerAdapter(
    private var yearList: List<Int>,
    private var yearInterface: YearInterface,
    private var context: Context
) : RecyclerView.Adapter<CustomYearPickerAdapter.ViewHolder>() {

    private var selectedYear = LocalDate.now().year

    class ViewHolder(val binding: ItemCustomMonthPickerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCustomMonthPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(yearList[absoluteAdapterPosition]) {
                val year = this.toString()
                binding.txtMonthName.text = year
            }

            if (selectedYear == yearList[absoluteAdapterPosition]) {
                binding.rvMonthContent.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_theme_oval)
                binding.txtMonthName.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.rvMonthContent.background = null
                binding.txtMonthName.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
            }

            binding.rvMonthContent.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < yearList.size) {
                    selectedYear = yearList[absoluteAdapterPosition]
                    yearInterface.onSelectedYearClick(yearList[absoluteAdapterPosition])
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return yearList.size
    }
}