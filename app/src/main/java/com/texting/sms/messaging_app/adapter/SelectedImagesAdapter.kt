package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.texting.sms.messaging_app.databinding.ItemSelectedImagesBinding
import com.texting.sms.messaging_app.listener.RemoveImageInterface

class SelectedImagesAdapter(
    private var imageUriList: MutableList<Uri>,
    private var removeImageInterface: RemoveImageInterface,
    private var context: Context
) : RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectedImagesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSelectedImagesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(imageUriList[position]) {
                Glide.with(context).load(this.toString().toUri()).into(binding.ivSelectedImages)
            }

            binding.ivRemoveImage.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < imageUriList.size) {
                    removeImageInterface.onRemoveImage(imageUriList[bindingAdapterPosition])
                    removeItemAt(bindingAdapterPosition)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return imageUriList.size
    }

    private fun removeItemAt(position: Int) {
        if (position in imageUriList.indices) {
            imageUriList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount - position)
        }
    }

    fun updateData(newList: MutableList<Uri>) {
        val diffCallback = SelectedImagesDiffCallback(imageUriList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        imageUriList.clear()
        imageUriList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class SelectedImagesDiffCallback(
    private val oldList: List<Uri>, private val newList: List<Uri>
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
