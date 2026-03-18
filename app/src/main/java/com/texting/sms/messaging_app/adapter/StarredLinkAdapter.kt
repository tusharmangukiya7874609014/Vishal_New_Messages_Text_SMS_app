package com.texting.sms.messaging_app.adapter

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.provider.BlockedNumberContract
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
import com.texting.sms.messaging_app.databinding.ItemStarredLinkBinding
import com.texting.sms.messaging_app.listener.OnStarredLinkClick
import com.texting.sms.messaging_app.model.MessageDetails
import com.texting.sms.messaging_app.utils.getColorFromAttr

class StarredLinkAdapter(
    private var starredLinkMessages: List<MessageDetails>,
    private var onStarredLinkClick: OnStarredLinkClick,
    private var context: Context
) : RecyclerView.Adapter<StarredLinkAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStarredLinkBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemStarredLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(starredLinkMessages[absoluteAdapterPosition]) {
                if (contactName != null) {
                    binding.txtLastMessage.text = contactName
                } else {
                    binding.txtLastMessage.text = address
                }

                val link = extractLinkFromMessage(body.toString())
                binding.txtMessageTitle.text = link

                itemView.setOnClickListener {
                    if (absoluteAdapterPosition != RecyclerView.NO_POSITION && absoluteAdapterPosition < starredLinkMessages.size) {
                        showContactPopup(it, this)
                    }
                }
            }
        }
    }

    private fun extractLinkFromMessage(message: String): String? {
        val urlPattern = "((https?://|http://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}([/?#]\\S*)?)"
            .toRegex(RegexOption.IGNORE_CASE)
        return urlPattern.find(message)?.value
    }

    override fun getItemCount(): Int {
        return starredLinkMessages.size
    }

    private fun showContactPopup(anchor: View, messageDetails: MessageDetails) {
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

        if (isAddressBlocked(context, messageDetails.address.toString())) {
            topViewBinding.cvProfileView.setCardBackgroundColor(context.getColor(R.color.blocked_user_profile))
            topViewBinding.txtUserName.setTextColor(context.getColor(R.color.blocked_user_profile))
            topViewBinding.ivOriginalProfile.visibility = View.GONE
            topViewBinding.ivDefaultProfile.visibility = View.GONE
            topViewBinding.ivBlockProfile.visibility = View.VISIBLE
        } else {
            if (messageDetails.photoUri != null && !messageDetails.photoUri.contentEquals("null")) {
                Glide.with(context)
                    .load(messageDetails.photoUri.toUri())
                    .into(topViewBinding.ivOriginalProfile)
                topViewBinding.ivDefaultProfile.visibility = View.GONE
                topViewBinding.ivBlockProfile.visibility = View.GONE
                topViewBinding.ivOriginalProfile.visibility = View.VISIBLE
            } else {
                topViewBinding.ivOriginalProfile.visibility = View.GONE
                topViewBinding.ivBlockProfile.visibility = View.GONE
                topViewBinding.ivDefaultProfile.visibility = View.VISIBLE
                topViewBinding.cvProfileView.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        context.getColorFromAttr(R.attr.itemBackgroundColor)
                    )
                )
            }
            topViewBinding.txtUserName.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
        }

        if (isAddressBlocked(context, messageDetails.address.toString())) {
            bottomViewBinding.cvProfileView.setCardBackgroundColor(context.getColor(R.color.blocked_user_profile))
            bottomViewBinding.txtUserName.setTextColor(context.getColor(R.color.blocked_user_profile))
            bottomViewBinding.ivOriginalProfile.visibility = View.GONE
            bottomViewBinding.ivDefaultProfile.visibility = View.GONE
            bottomViewBinding.ivBlockProfile.visibility = View.VISIBLE
        } else {
            if (messageDetails.photoUri != null && !messageDetails.photoUri.contentEquals("null")) {
                Glide.with(context)
                    .load(messageDetails.photoUri.toUri())
                    .into(bottomViewBinding.ivOriginalProfile)
                bottomViewBinding.ivDefaultProfile.visibility = View.GONE
                bottomViewBinding.ivBlockProfile.visibility = View.GONE
                bottomViewBinding.ivOriginalProfile.visibility = View.VISIBLE
            } else {
                bottomViewBinding.ivOriginalProfile.visibility = View.GONE
                bottomViewBinding.ivBlockProfile.visibility = View.GONE
                bottomViewBinding.ivDefaultProfile.visibility = View.VISIBLE
                bottomViewBinding.cvProfileView.setCardBackgroundColor(
                    ColorStateList.valueOf(
                        context.getColorFromAttr(R.attr.itemBackgroundColor)
                    )
                )
            }
            bottomViewBinding.txtUserName.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
        }

        topViewBinding.txtUserName.text =
            if (messageDetails.contactName?.isNotEmpty() == true) messageDetails.contactName else messageDetails.address
        topViewBinding.txtUserNumber.text = messageDetails.address

        topViewBinding.txtCopyTitle.text = context.getString(R.string.copy_link)
        bottomViewBinding.txtCopyTitle.text = context.getString(R.string.copy_link)

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

        topViewBinding.rvOpen.visibility = View.VISIBLE
        topViewBinding.rvOpenView.visibility = View.VISIBLE
        bottomViewBinding.rvOpen.visibility = View.VISIBLE
        bottomViewBinding.rvOpenView.visibility = View.VISIBLE

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

        topViewBinding.rvOpen.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "OPEN")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvOpen.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "OPEN")
            popupWindow.dismiss()
        }

        topViewBinding.rvCallAction.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "CALL")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCallAction.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "CALL")
            popupWindow.dismiss()
        }

        topViewBinding.rvWhatsapp.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "WHATSAPP_MESSAGE")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvWhatsapp.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "WHATSAPP_MESSAGE")
            popupWindow.dismiss()
        }

        topViewBinding.rvCopyMessage.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "COPY_LINK")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCopyMessage.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "COPY_LINK")
            popupWindow.dismiss()
        }

        topViewBinding.rvSendMessage.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "SEND_MESSAGES")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvSendMessage.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "SEND_MESSAGES")
            popupWindow.dismiss()
        }

        topViewBinding.rvViewInChat.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "VIEW_IN_CHAT")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvViewInChat.setOnClickListener {
            onStarredLinkClick.onLinkMessageClick(messageDetails, "VIEW_IN_CHAT")
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(rootView, Gravity.TOP or Gravity.START, 34, yPos)
    }

    private fun isAddressBlocked(context: Context?, number: String): Boolean {
        return try {
            BlockedNumberContract.isBlocked(context, number)
        } catch (_: Exception) {
            false
        }
    }

    private fun isPhoneNumber(address: String?): Boolean {
        if (address.isNullOrBlank()) return false
        return PhoneNumberUtils.isGlobalPhoneNumber(address)
    }
}

