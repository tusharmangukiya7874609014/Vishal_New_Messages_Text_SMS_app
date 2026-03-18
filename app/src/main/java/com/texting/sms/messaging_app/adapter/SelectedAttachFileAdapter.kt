package com.texting.sms.messaging_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.databinding.ItemDocumentAttachBinding
import com.texting.sms.messaging_app.listener.RemoveFileInterface
import com.texting.sms.messaging_app.model.AttachFile

class SelectedAttachFileAdapter(
    private var attachFileList: MutableList<AttachFile>,
    private var removeFileInterface: RemoveFileInterface
) : RecyclerView.Adapter<SelectedAttachFileAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemDocumentAttachBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemDocumentAttachBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(attachFileList[absoluteAdapterPosition]) {
                binding.txtFileName.text = fileName
                binding.txtFileSize.text = fileSize
            }

            binding.ivRemoveImage.setOnClickListener {
                if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < attachFileList.size) {
                    removeFileInterface.onItemClick(attachFileList[absoluteAdapterPosition])
                    removeItemAt(absoluteAdapterPosition)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return attachFileList.size
    }

    private fun removeItemAt(position: Int) {
        if (position in attachFileList.indices) {
            attachFileList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount - position)
        }
    }

    fun updateData(newList: MutableList<AttachFile>) {
        val diffCallback = SelectedAttachFileDiffCallback(attachFileList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        attachFileList.clear()
        attachFileList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class SelectedAttachFileDiffCallback(
    private val oldList: List<AttachFile>, private val newList: List<AttachFile>
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
