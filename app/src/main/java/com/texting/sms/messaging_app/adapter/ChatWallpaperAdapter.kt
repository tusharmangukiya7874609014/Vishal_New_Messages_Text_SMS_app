package com.texting.sms.messaging_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ItemChatWallpaperBinding
import com.texting.sms.messaging_app.listener.OnChatWallpaperClickInterface
import com.texting.sms.messaging_app.model.ChatWallpaper

class ChatWallpaperAdapter(
    private var chatWallpaperList: MutableList<ChatWallpaper>,
    private var onChatWallpaperClickInterface: OnChatWallpaperClickInterface,
    context: Context
) : RecyclerView.Adapter<ChatWallpaperAdapter.ViewHolder>() {

    private var wallpaperType =
        SharedPreferencesHelper.getString(context, Const.WALLPAPER_TYPE, "Default")
    private var selectedPosition = if (wallpaperType.contentEquals("Others")) {
        SharedPreferencesHelper.getInt(context, Const.OTHERS_WALLPAPER_POSITION, -1)
    } else {
        -1
    }
    private var lastSelectedPosition = -1

    class ViewHolder(val binding: ItemChatWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemChatWallpaperBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            binding.executePendingBindings()

            if (selectedPosition == bindingAdapterPosition) {
                chatWallpaperList[position].isWallpaperSelected = true
                lastSelectedPosition = position
            } else {
                chatWallpaperList[position].isWallpaperSelected = false
            }

            binding.item = chatWallpaperList[position]

            itemView.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < chatWallpaperList.size) {
                    selectedPosition = bindingAdapterPosition

                    onChatWallpaperClickInterface.onChatWallpaperClick(
                        bindingAdapterPosition,
                        chatWallpaperList[bindingAdapterPosition]
                    )
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            holder.binding.item = chatWallpaperList[position]
        } else {
            onBindViewHolder(holder, position)
        }
    }

    fun updateSelectedChatWallpaper(selectedPosition: Int) {
        /**  Update last selected position  **/
        if (lastSelectedPosition != -1) {
            chatWallpaperList[lastSelectedPosition].isWallpaperSelected = false
            notifyItemChanged(lastSelectedPosition, "payload_update")
        }

        /**  Update new selected position  **/
        if (selectedPosition != -1) {
            lastSelectedPosition = selectedPosition
            chatWallpaperList[selectedPosition].isWallpaperSelected = true
            notifyItemChanged(selectedPosition, "payload_update")
        }
    }

    override fun getItemCount(): Int {
        return chatWallpaperList.size
    }

    fun updateData(newList: List<ChatWallpaper>) {
        val diffCallback = ChatWallpaperDiffCallback(chatWallpaperList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        chatWallpaperList.clear()
        chatWallpaperList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }
}

class ChatWallpaperDiffCallback(
    private val oldList: List<ChatWallpaper>, private val newList: List<ChatWallpaper>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}