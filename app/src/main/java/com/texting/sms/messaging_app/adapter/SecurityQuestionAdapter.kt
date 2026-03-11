package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemQuestionsListBinding
import com.texting.sms.messaging_app.listener.OnClickQuestionInterface
import com.texting.sms.messaging_app.model.Question
import com.texting.sms.messaging_app.utils.getColorFromAttr

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