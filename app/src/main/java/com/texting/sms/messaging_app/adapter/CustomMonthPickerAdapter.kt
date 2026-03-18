package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemCustomMonthPickerBinding
import com.texting.sms.messaging_app.listener.MonthInterface
import com.texting.sms.messaging_app.model.MonthFile
import com.texting.sms.messaging_app.utils.getColorFromAttr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CustomMonthPickerAdapter(
    private var monthList: List<MonthFile>,
    private var monthInterface: MonthInterface,
    private var context: Context
) : RecyclerView.Adapter<CustomMonthPickerAdapter.ViewHolder>() {

    private var selectedMonth = getCurrentMonthShortName()

    class ViewHolder(val binding: ItemCustomMonthPickerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemCustomMonthPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(monthList[absoluteAdapterPosition]) {
                binding.txtMonthName.text = monthSort
            }

            if (selectedMonth == monthList[absoluteAdapterPosition].monthSort) {
                binding.rvMonthContent.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_theme_oval)
                binding.txtMonthName.setTextColor(ContextCompat.getColor(context, R.color.white))
            } else {
                binding.rvMonthContent.background = null
                binding.txtMonthName.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
            }

            binding.rvMonthContent.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < monthList.size) {
                    selectedMonth = monthList[absoluteAdapterPosition].monthSort
                    monthInterface.onSelectedMonthClick(monthList[absoluteAdapterPosition])
                    notifyDataSetChanged()
                }
            }
        }
    }

    private fun getCurrentMonthShortName(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
        return dateFormat.format(calendar.time)
    }

    override fun getItemCount(): Int {
        return monthList.size
    }
}