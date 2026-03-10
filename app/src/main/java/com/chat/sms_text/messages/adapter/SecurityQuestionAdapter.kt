package com.chat.sms_text.messages.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.databinding.ItemQuestionsListBinding
import com.chat.sms_text.messages.listener.OnClickQuestionInterface
import com.chat.sms_text.messages.model.Question
import com.chat.sms_text.messages.utils.getColorFromAttr

class SecurityQuestionAdapter(
    private var questionList: List<Question>,
    private var questionInterface: OnClickQuestionInterface,
    private var context: Context
) : RecyclerView.Adapter<SecurityQuestionAdapter.ViewHolder>() {

    private var rowIndex = 0

    class ViewHolder(val binding: ItemQuestionsListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemQuestionsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(questionList[position]) {
                binding.txtQuestion.text = this.question
            }

            binding.root.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < questionList.size) {
                    rowIndex = bindingAdapterPosition
                    questionInterface.onItemClick(questionList[bindingAdapterPosition].question.toString())
                    notifyDataSetChanged()
                }
            }

            if (rowIndex == bindingAdapterPosition) {
                binding.txtQuestion.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.app_theme_color
                    )
                )
            } else {
                binding.txtQuestion.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
            }
        }
    }

    override fun getItemCount(): Int {
        return questionList.size
    }
}