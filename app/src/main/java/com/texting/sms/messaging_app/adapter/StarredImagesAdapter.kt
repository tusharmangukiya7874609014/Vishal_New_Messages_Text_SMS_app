package com.texting.sms.messaging_app.adapter

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.databinding.ItemMessageFeaturesBottomViewPopupBinding
import com.texting.sms.messaging_app.databinding.ItemMessageFeaturesTopViewPopupBinding
import com.texting.sms.messaging_app.databinding.ItemStarredImagesBinding
import com.texting.sms.messaging_app.listener.OnStarredImageClick
import com.texting.sms.messaging_app.model.MmsContactInfo
import com.texting.sms.messaging_app.utils.getColorFromAttr

class StarredImagesAdapter(
    private var starredImages: MutableSet<String>,
    private var onStarredImageClick: OnStarredImageClick,
    private var context: Context
) : RecyclerView.Adapter<StarredImagesAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStarredImagesBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStarredImagesBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(starredImages.elementAt(absoluteAdapterPosition)) {
                val regex = Regex("^\\d+")
                val messageIdForImage = regex.find(this)?.value ?: ""

                val photoUri = "content://mms/part/$messageIdForImage"
                Glide.with(context).load(photoUri.toUri()).into(binding.ivSelectedImages)

                itemView.setOnClickListener {
                    if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < starredImages.size) {
                        showContactPopup(it, messageIdForImage)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return starredImages.size
    }

    private fun showContactPopup(anchor: View, messagesID: String) {
        val messageDetails = getMmsPartDetails(context, messagesID.toLong()) ?: return
        val activity = anchor.context as Activity
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

        val topViewBinding =
            ItemMessageFeaturesTopViewPopupBinding.inflate(LayoutInflater.from(anchor.context))
        val bottomViewBinding =
            ItemMessageFeaturesBottomViewPopupBinding.inflate(LayoutInflater.from(anchor.context))

        var popupView = topViewBinding.root

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        val dimView = View(anchor.context).apply {
            setBackgroundColor("#73000000".toColorInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurEffect = RenderEffect.createBlurEffect(50f, 50f, Shader.TileMode.CLAMP)
            rootView.setRenderEffect(blurEffect)
            rootView.addView(dimView)
        } else {
            popupWindow.setBackgroundDrawable("#73000000".toColorInt().toDrawable())
            popupWindow.isOutsideTouchable = true
            popupWindow.isFocusable = true
        }

        popupWindow.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                rootView.setRenderEffect(null)
                rootView.removeView(dimView)
            }
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupHeight = popupView.measuredHeight
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorY = location[1]
        val anchorHeight = anchor.height

        val spaceBelow = screenHeight - (anchorY + anchorHeight)

        popupView = if (spaceBelow >= popupHeight) {
            topViewBinding.root
        } else if (anchorY >= popupHeight) {
            bottomViewBinding.root
        } else {
            topViewBinding.root
        }

        popupWindow.contentView = popupView

        val yPos = when {
            spaceBelow >= popupHeight -> anchorY + anchorHeight
            anchorY >= popupHeight -> anchorY - popupHeight
            else -> screenHeight / 2 - popupHeight / 2
        }

        topViewBinding.userContactAddress = messageDetails.address
        bottomViewBinding.userContactAddress = messageDetails.address

        topViewBinding.txtUserName.text =
            if (messageDetails.contactName?.isNotEmpty() == true) messageDetails.contactName else messageDetails.address
        topViewBinding.txtUserNumber.text = messageDetails.address

        if (isPhoneNumber(messageDetails.address)) {
            topViewBinding.txtCall.text =
                activity.getString(R.string.call_auto, messageDetails.address)
            topViewBinding.rvCallAction.visibility = View.VISIBLE
            topViewBinding.rvCallActionView.visibility = View.VISIBLE
            topViewBinding.rvSendMessage.visibility = View.VISIBLE
            topViewBinding.rvSendMessageView.visibility = View.VISIBLE
            topViewBinding.rvWhatsapp.visibility = View.VISIBLE
            topViewBinding.rvWhatsappView.visibility = View.VISIBLE
        } else {
            topViewBinding.rvCallAction.visibility = View.GONE
            topViewBinding.rvCallActionView.visibility = View.GONE
            topViewBinding.rvSendMessage.visibility = View.GONE
            topViewBinding.rvSendMessageView.visibility = View.GONE
            topViewBinding.rvWhatsapp.visibility = View.GONE
            topViewBinding.rvWhatsappView.visibility = View.GONE
        }

        bottomViewBinding.txtUserName.text =
            if (messageDetails.contactName?.isNotEmpty() == true) messageDetails.contactName else messageDetails.address
        bottomViewBinding.txtUserNumber.text = messageDetails.address

        if (isPhoneNumber(messageDetails.address)) {
            bottomViewBinding.txtCall.text =
                activity.getString(R.string.call_auto, messageDetails.address)
            bottomViewBinding.rvCallAction.visibility = View.VISIBLE
            bottomViewBinding.rvCallActionView.visibility = View.VISIBLE
            bottomViewBinding.rvSendMessage.visibility = View.VISIBLE
            bottomViewBinding.rvSendMessageView.visibility = View.VISIBLE
            bottomViewBinding.rvWhatsapp.visibility = View.VISIBLE
            bottomViewBinding.rvWhatsappView.visibility = View.VISIBLE
        } else {
            bottomViewBinding.rvCallAction.visibility = View.GONE
            bottomViewBinding.rvCallActionView.visibility = View.GONE
            bottomViewBinding.rvSendMessage.visibility = View.GONE
            bottomViewBinding.rvSendMessageView.visibility = View.GONE
            bottomViewBinding.rvWhatsapp.visibility = View.GONE
            bottomViewBinding.rvWhatsappView.visibility = View.GONE
        }

        topViewBinding.rvCopyMessage.visibility = View.GONE
        topViewBinding.rvCopyView.visibility = View.GONE

        bottomViewBinding.rvCopyMessage.visibility = View.GONE
        bottomViewBinding.rvCopyView.visibility = View.GONE


        topViewBinding.rvCallAction.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "CALL", messageDetails)
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCallAction.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "CALL", messageDetails)
            popupWindow.dismiss()
        }

        topViewBinding.rvWhatsapp.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "WHATSAPP_MESSAGE", messageDetails)
            popupWindow.dismiss()
        }

        bottomViewBinding.rvWhatsapp.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "WHATSAPP_MESSAGE", messageDetails)
            popupWindow.dismiss()
        }

        topViewBinding.rvSendMessage.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "SEND_MESSAGES", messageDetails)
            popupWindow.dismiss()
        }

        bottomViewBinding.rvSendMessage.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "SEND_MESSAGES", messageDetails)
            popupWindow.dismiss()
        }

        topViewBinding.rvViewInChat.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "VIEW_IN_CHAT", messageDetails)
            popupWindow.dismiss()
        }

        bottomViewBinding.rvViewInChat.setOnClickListener {
            onStarredImageClick.onImagesClick(messagesID, "VIEW_IN_CHAT", messageDetails)
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(rootView, Gravity.TOP or Gravity.START, 34, yPos)
    }

    private fun isPhoneNumber(address: String?): Boolean {
        if (address.isNullOrBlank()) return false
        return PhoneNumberUtils.isGlobalPhoneNumber(address)
    }

    private fun isAddressBlocked(context: Context?, number: String): Boolean {
        return try {
            BlockedNumberContract.isBlocked(context, number)
        } catch (_: Exception) {
            false
        }
    }

    private fun getMmsPartDetails(context: Context, partId: Long): MmsContactInfo? {
        val uri = "content://mms/part/$partId".toUri()
        val projection = arrayOf("_id", "mid")

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val mmsId = cursor.getLong(cursor.getColumnIndexOrThrow("mid"))
                return getMmsContactInfo(context, mmsId)
            }
        }
        return null
    }

    private fun getMmsContactInfo(context: Context, mmsId: Long): MmsContactInfo {
        val address = getMmsAddress(context, mmsId)
        var contactName: String? = null
        var photoUri: String? = null

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address)
        )

        context.contentResolver.query(
            uri, arrayOf(
                ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI
            ), null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactName = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                )
                photoUri = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                )
            }
        }

        return MmsContactInfo(contactName, address, photoUri)
    }

    private fun getMmsAddress(context: Context, mmsId: Long): String {
        val uri = "content://mms/$mmsId/addr".toUri()
        val cursor = context.contentResolver.query(
            uri, arrayOf("address"), null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                if (!address.isNullOrEmpty() && address != "insert-address-token") {
                    return address
                }
            }
        }
        return "Unknown"
    }
}