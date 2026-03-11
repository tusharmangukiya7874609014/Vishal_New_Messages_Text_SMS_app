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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.texting.sms.messaging_app.R
import com.texting.sms.messaging_app.database.Const
import com.texting.sms.messaging_app.database.SharedPreferencesHelper
import com.texting.sms.messaging_app.databinding.ItemMessageFeaturesBottomViewPopupBinding
import com.texting.sms.messaging_app.databinding.ItemMessageFeaturesTopViewPopupBinding
import com.texting.sms.messaging_app.databinding.ItemStarredPlainMessageBinding
import com.texting.sms.messaging_app.listener.OnStarredTextClick
import com.texting.sms.messaging_app.model.MessageDetails
import com.texting.sms.messaging_app.utils.getColorFromAttr
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StarredPlainMessageAdapter(
    private var starredMessages: List<MessageDetails>,
    private var onStarredTextClick: OnStarredTextClick,
    private var context: Context
) : RecyclerView.Adapter<StarredPlainMessageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemStarredPlainMessageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemStarredPlainMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            with(starredMessages[position]) {
                if (contactName != null) {
                    binding.txtMessageTitle.text = contactName
                } else {
                    binding.txtMessageTitle.text = address
                }

                binding.txtLastMessage.text = body
                val formattedTime = formatMessageTime(date)
                binding.txtDate.text = formattedTime

                if (isAddressBlocked(context, address.toString())) {
                    binding.cvProfileView.setCardBackgroundColor(context.getColor(R.color.blocked_user_profile))
                    binding.txtMessageTitle.setTextColor(context.getColor(R.color.blocked_user_profile))
                    binding.ivOriginalProfile.visibility = View.GONE
                    binding.ivDefaultProfile.visibility = View.GONE
                    binding.ivBlockProfile.visibility = View.VISIBLE
                } else {
                    if (photoUri != null && !photoUri.contentEquals("null")) {
                        Glide.with(context)
                            .load(photoUri.toUri())
                            .into(binding.ivOriginalProfile)
                        binding.ivDefaultProfile.visibility = View.GONE
                        binding.ivBlockProfile.visibility = View.GONE
                        binding.ivOriginalProfile.visibility = View.VISIBLE
                    } else {
                        if (SharedPreferencesHelper.getBoolean(
                                context,
                                Const.IS_CHANGE_PROFILE_COLOR,
                                false
                            )
                        ) {
                            binding.ivDefaultProfile.setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_profile
                                )
                            )
                        } else {
                            binding.ivDefaultProfile.setImageDrawable(
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_dark_profile_popup
                                )
                            )
                        }
                        binding.ivOriginalProfile.visibility = View.GONE
                        binding.ivBlockProfile.visibility = View.GONE
                        binding.cvProfileView.setCardBackgroundColor(
                            ColorStateList.valueOf(
                                context.getColorFromAttr(R.attr.itemBackgroundColor)
                            )
                        )
                        binding.ivDefaultProfile.visibility = View.VISIBLE
                    }
                    binding.txtMessageTitle.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
                }

                itemView.setOnClickListener {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION && bindingAdapterPosition < starredMessages.size) {
                        showContactPopup(it, this)
                    }
                }
            }
        }
    }

    private fun formatMessageTime(timestamp: Long): String {
        val messageCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val now = Calendar.getInstance()

        return when {
            // Today: show time like 4:40 PM
            now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == messageCal.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(timestamp))
            }

            // Same week: show day name like Mon, Tue
            now.get(Calendar.WEEK_OF_YEAR) == messageCal.get(Calendar.WEEK_OF_YEAR) &&
                    now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("EEE", Locale.ENGLISH).format(Date(timestamp))
            }

            // Same month: show like May 10
            now.get(Calendar.MONTH) == messageCal.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == messageCal.get(Calendar.YEAR) -> {
                SimpleDateFormat("MMM d", Locale.ENGLISH).format(Date(timestamp))
            }

            // Different year: show full date like 10/05/2023
            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(timestamp))
            }
        }
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

    override fun getItemCount(): Int {
        return starredMessages.size
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

        topViewBinding.rvCallAction.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "CALL")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCallAction.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "CALL")
            popupWindow.dismiss()
        }

        topViewBinding.rvWhatsapp.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "WHATSAPP_MESSAGE")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvWhatsapp.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "WHATSAPP_MESSAGE")
            popupWindow.dismiss()
        }

        topViewBinding.rvCopyMessage.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "COPY_MESSAGE")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvCopyMessage.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "COPY_MESSAGE")
            popupWindow.dismiss()
        }

        topViewBinding.rvSendMessage.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "SEND_MESSAGES")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvSendMessage.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "SEND_MESSAGES")
            popupWindow.dismiss()
        }

        topViewBinding.rvViewInChat.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "VIEW_IN_CHAT")
            popupWindow.dismiss()
        }

        bottomViewBinding.rvViewInChat.setOnClickListener {
            onStarredTextClick.onPlainTextMessageClick(messageDetails, "VIEW_IN_CHAT")
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(rootView, Gravity.TOP or Gravity.START, 34, yPos)
    }
}