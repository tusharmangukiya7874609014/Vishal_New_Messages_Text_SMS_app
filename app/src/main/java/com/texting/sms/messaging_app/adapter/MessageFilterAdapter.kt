package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemMessageFilterBinding
import com.texting.sms.messaging_app.listener.OnMessageFilterInterface
import com.texting.sms.messaging_app.model.MessageFilter
import com.texting.sms.messaging_app.utils.getColorFromAttr
import com.texting.sms.messaging_app.utils.getDrawableFromAttr

class MessageFilterAdapter(
    private var messageFilterList: List<MessageFilter>,
    private var onMessageFilterInterface: OnMessageFilterInterface,
    private var context: Context
) : RecyclerView.Adapter<MessageFilterAdapter.ViewHolder>() {

    private var rowIndex = 0
    private var unReadCount = 0
    private var allMessagesCount = 0

    class ViewHolder(val binding: ItemMessageFilterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemMessageFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(messageFilterList[absoluteAdapterPosition]) {
                binding.ivFilterIcon.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        context,
                        filterImage
                    )
                )

                when (absoluteAdapterPosition) {
                    0 -> {
                        if (allMessagesCount != 0) {
                            val finalCount =
                                context.resources.getString(R.string.all_messages) + " (" + allMessagesCount + ")"
                            binding.txtFilterName.text = finalCount
                        } else {
                            binding.txtFilterName.text = filterName
                        }
                    }

                    1 -> {
                        if (unReadCount != 0) {
                            val finalCount =
                                context.resources.getString(R.string.unread) + " (" + unReadCount + ")"
                            binding.txtFilterName.text = finalCount
                        } else {
                            binding.txtFilterName.text = filterName
                        }
                    }

                    else -> {
                        binding.txtFilterName.text = filterName
                    }
                }
            }

            val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
            if (absoluteAdapterPosition == messageFilterList.size - 1) {
                params.marginEnd = 0
            } else {
                params.marginEnd = context.resources.getDimensionPixelSize(R.dimen._8sdp)
            }
            holder.itemView.layoutParams = params

            itemView.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < messageFilterList.size) {
                    rowIndex = absoluteAdapterPosition
                    onMessageFilterInterface.onFilterClick(messageFilterList[absoluteAdapterPosition])
                    notifyDataSetChanged()
                }
            }

            if (rowIndex == absoluteAdapterPosition) {
                binding.rvMainContentView.setBorder(
                    ContextCompat.getColor(
                        context, R.color.app_theme_color
                    ), 3
                )
                binding.ivFilterIcon.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.app_theme_color))
                binding.txtFilterName.setTextColor(
                    ContextCompat.getColor(
                        context, R.color.app_theme_color
                    )
                )
            } else {
                binding.rvMainContentView.background =
                    context.getDrawableFromAttr(R.attr.itemBackground)
                binding.ivFilterIcon.backgroundTintList = null
                binding.txtFilterName.setTextColor(context.getColorFromAttr(R.attr.subTextColor))
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            if (payloads[0] == "unread_message_count") {
                if (unReadCount != 0) {
                    val finalCount =
                        context.resources.getString(R.string.unread) + " (" + unReadCount + ")"
                    holder.binding.txtFilterName.text = finalCount
                } else {
                    holder.binding.txtFilterName.text = context.resources.getString(R.string.unread)
                }
            }
            if (payloads[0] == "all_message_count") {
                if (allMessagesCount != 0) {
                    val finalCount =
                        context.resources.getString(R.string.all_messages) + " (" + allMessagesCount + ")"
                    holder.binding.txtFilterName.text = finalCount
                } else {
                    holder.binding.txtFilterName.text =
                        context.resources.getString(R.string.all_messages)
                }
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return messageFilterList.size
    }

    fun updateIconOfUnread(count: Int) {
        unReadCount = count
        notifyItemChanged(1, "unread_message_count")
    }

    fun allMessageOfCount(count: Int) {
        allMessagesCount = count
        notifyItemChanged(0, "all_message_count")
    }

    private fun View.setBorder(
        borderColor: Int,
        borderWidth: Int,
        cornerRadius: Float = 15f,
        backgroundColor: Int = context.getColorFromAttr(R.attr.itemBackgroundColor)
    ) {
        val drawable = GradientDrawable().apply {
            setColor(backgroundColor)
            setStroke(borderWidth, borderColor)
            this.cornerRadius = cornerRadius
        }
        background = drawable
    }
}