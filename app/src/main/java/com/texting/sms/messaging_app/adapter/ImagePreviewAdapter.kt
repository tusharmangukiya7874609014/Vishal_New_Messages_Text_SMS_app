package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.texting.sms.messaging_app.databinding.ItemPreviewSelectedImagesBinding

class ImagePreviewAdapter(
    private var mmsImagePreviewList: MutableList<Uri>,
    private var context: Context
) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemPreviewSelectedImagesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemPreviewSelectedImagesBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(mmsImagePreviewList[absoluteAdapterPosition]) {
                Glide.with(context)
                    .load(this)
                    .into(binding.ivSelectedImage)
            }
        }
    }

    override fun getItemCount(): Int {
        return mmsImagePreviewList.size
    }
}