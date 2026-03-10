package com.chat.sms_text.messages.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chat.sms_text.messages.databinding.ItemSelectedImagesBinding

class ChatHistoryImageAdapter(
    private var imageUriList: MutableList<Uri>,
    private var context: Context,
    private val onImageClick: ((Uri) -> Unit)? = null
) : RecyclerView.Adapter<ChatHistoryImageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemSelectedImagesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemSelectedImagesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.ivRemoveImage.visibility = View.GONE
            with(imageUriList[position]) {
                Glide.with(context).load(this.toString().toUri())
                    .into(binding.ivSelectedImages)

                binding.root.setOnClickListener {
                    onImageClick?.invoke(this.toString().toUri())
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return imageUriList.size
    }
}