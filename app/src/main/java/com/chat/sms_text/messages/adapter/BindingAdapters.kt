package com.chat.sms_text.messages.adapter

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.chat.sms_text.messages.R
import com.chat.sms_text.messages.database.Const
import com.chat.sms_text.messages.database.SharedPreferencesHelper
import com.chat.sms_text.messages.utils.ContactNameCache
import com.chat.sms_text.messages.utils.ContactNumberCache
import com.chat.sms_text.messages.utils.MessageOTPCache
import com.chat.sms_text.messages.utils.MessageTimeCache
import com.chat.sms_text.messages.utils.ProfileCache
import com.chat.sms_text.messages.utils.getColorFromAttr
import com.chat.sms_text.messages.utils.getDrawableFromAttr

@BindingAdapter("languageTextColor")
fun setLanguageTextColor(view: TextView, isSelected: Boolean) {
    val color = if (isSelected) {
        ContextCompat.getColor(view.context, R.color.app_theme_color)
    } else {
        view.context.getColorFromAttr(R.attr.subTextColor)
    }
    view.setTextColor(color)
}

@BindingAdapter("selectedLanguage")
fun View.setLanguageBorder(isSelected: Boolean) {
    if (isSelected) {
        val drawable = GradientDrawable().apply {
            setColor(context.getColorFromAttr(R.attr.itemBackgroundColor))
            setStroke(
                2,
                ContextCompat.getColor(context, R.color.app_theme_color)
            )
            cornerRadius = 15f
        }
        background = drawable
    } else {
        background = context.getDrawableFromAttr(R.attr.itemBackground)
    }
}

@BindingAdapter("chatBoxStyleImageSrc")
fun setChatImage(view: AppCompatImageView, drawableRes: Int?) {
    drawableRes?.let {
        Glide.with(view.context)
            .load(it)
            .dontAnimate()
            .into(view)
    }
}

@BindingAdapter("selectedTab")
fun View.setTabBorder(isSelected: Boolean) {
    if (isSelected) {
        val drawable = GradientDrawable().apply {
            setColor(context.getColorFromAttr(R.attr.itemBackgroundColor))
            setStroke(
                2,
                ContextCompat.getColor(context, R.color.app_theme_color)
            )
            cornerRadius = 15f
        }
        background = drawable
    } else {
        background = context.getDrawableFromAttr(R.attr.itemBackground)
    }
}

@BindingAdapter("tabTextColor")
fun setTabTextColor(view: TextView, isSelected: Boolean) {
    val color = if (isSelected) {
        ContextCompat.getColor(view.context, R.color.app_theme_color)
    } else {
        view.context.getColorFromAttr(R.attr.subTextColor)
    }
    view.setTextColor(color)
}

@BindingAdapter("chatBoxColorRes")
fun setChatBoxColor(view: View, @ColorRes colorRes: Int?) {
    if (colorRes == null) return

    view.setBackgroundColor(
        ContextCompat.getColor(view.context, colorRes)
    )
}

@BindingAdapter("contactId")
fun TextView.bindContactNumber(contactId: String?) {
    if (contactId.isNullOrEmpty()) {
        text = ""
        visibility = View.GONE
        return
    }

    val number = ContactNumberCache.getNumber(contactId)

    if (number.isNullOrEmpty()) {
        text = ""
        visibility = View.GONE
    } else {
        text = number
        visibility = View.VISIBLE
    }
}

@BindingAdapter(
    value = ["profilePhotoUri", "isSelected"],
    requireAll = false
)
fun AppCompatImageView.bindProfilePhoto(
    photoUri: String?,
    isSelected: Boolean = false
) {
    val root = (parent as? ViewGroup)?.parent as? ViewGroup
    val defaultImage = root?.findViewById<AppCompatImageView>(R.id.ivDefaultProfile)
    val cardView = root?.findViewById<CardView>(R.id.cvProfileView)

    visibility = View.GONE
    defaultImage?.visibility = View.GONE
    Glide.with(context).clear(this)

    val ctx = context

    if (isSelected) {
        visibility = View.VISIBLE
        setImageResource(R.drawable.ic_profile_selected)
        cardView?.setCardBackgroundColor(
            ColorStateList.valueOf(
                ctx.getColorFromAttr(R.attr.itemBackgroundColor)
            )
        )
        return
    }

    if (!photoUri.isNullOrEmpty() && photoUri != "null") {
        visibility = View.VISIBLE

        Glide.with(ctx)
            .load(photoUri.toUri())
            .dontAnimate()
            .into(this)
        return
    }

    defaultImage?.apply {
        visibility = View.VISIBLE

        val drawableRes = if (
            SharedPreferencesHelper.getBoolean(
                ctx,
                Const.IS_CHANGE_PROFILE_COLOR,
                false
            )
        ) {
            R.drawable.ic_profile
        } else {
            R.drawable.ic_dark_profile_popup
        }

        setImageDrawable(ContextCompat.getDrawable(ctx, drawableRes))
    }

    cardView?.setCardBackgroundColor(
        ColorStateList.valueOf(
            ctx.getColorFromAttr(R.attr.itemBackgroundColor)
        )
    )
}

@BindingAdapter("otpMessage")
fun LinearLayout.bindOtpMessage(message: String?) {
    val txtCode = findViewById<TextView>(R.id.txtCode) ?: return

    if (message.isNullOrBlank()) {
        tag = null
        visibility = View.GONE
        return
    }

    var detectedOtp = MessageOTPCache.getLatestMessageOTP(message)

    if (detectedOtp == null) {
        detectedOtp = MessageOTPCache.extractOtpFromLatestMessage(message)
        MessageOTPCache.putLatestMessageOTP(message, detectedOtp)
    }

    if (detectedOtp.isNullOrEmpty()) {
        tag = null
        visibility = View.GONE
    } else {
        tag = detectedOtp
        txtCode.text = context.getString(R.string.code_copy, detectedOtp)
        visibility = View.VISIBLE
    }
}

@BindingAdapter("contactName")
fun TextView.bindContactName(address: String?) {
    if (address.isNullOrBlank()) {
        text = ""
        return
    }

    var name = ContactNameCache.getName(address)

    if (name == null) {
        name = ContactNameCache.extractName(context, address)
        ContactNameCache.putName(address, name)
    }

    text = name ?: address
}

@BindingAdapter("messageTime")
fun TextView.bindMessageTime(timestamp: Long?) {
    if (timestamp == null || timestamp <= 0L) {
        text = ""
        return
    }

    var formattedTime = MessageTimeCache.getFormattedTime(timestamp)

    if (formattedTime == null) {
        formattedTime = MessageTimeCache.formatMessageTime(timestamp)
        MessageTimeCache.putFormattedTime(timestamp, formattedTime)
    }

    text = formattedTime
}

@BindingAdapter(
    value = ["profileMessage", "profileCard", "blockedProfile", "messageTitle"],
    requireAll = true
)
fun AppCompatImageView.bindProfile(
    address: String?, cvProfileView: CardView,
    blockedProfile: AppCompatImageView,
    messageTitle: TextView
) {
    if (address.isNullOrBlank()) {
        visibility = View.GONE
        return
    }

    val context = context

    var photoUri = ProfileCache.getPhoto(address)
    if (photoUri == null) {
        photoUri = ProfileCache.getOrLoadPhoto(context, address)
    }

    val isBlocked = ProfileCache.isAddressBlocked(context, address)
    if (isBlocked) {
        visibility = View.GONE
        messageTitle.setTextColor(context.getColor(R.color.blocked_user_profile))
        cvProfileView.setCardBackgroundColor(context.getColor(R.color.blocked_user_profile))
        blockedProfile.setImageResource(R.drawable.ic_block)
        blockedProfile.visibility = View.VISIBLE
        return
    }

    blockedProfile.visibility = View.GONE
    cvProfileView.setCardBackgroundColor(context.getColorFromAttr(R.attr.itemBackgroundColor))
    messageTitle.setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
    visibility = View.VISIBLE

    if (!photoUri.isNullOrBlank() && photoUri != "null") {
        Glide.with(context)
            .load(photoUri.toUri())
            .dontAnimate()
            .into(this)
    } else {
        val res = if (
            SharedPreferencesHelper.getBoolean(
                context,
                Const.IS_CHANGE_PROFILE_COLOR,
                false
            )
        ) {
            R.drawable.ic_profile
        } else {
            R.drawable.ic_dark_profile_popup
        }

        setImageResource(res)
    }
}

@BindingAdapter("unreadTitleFont")
fun TextView.bindUnreadTitle(unreadCount: Int) {
    if (unreadCount > 0) {
        setTypeface(null, Typeface.BOLD)
        ResourcesCompat.getFont(context, R.font.medium_sans)
    } else {
        setTypeface(null, Typeface.NORMAL)
        ResourcesCompat.getFont(context, R.font.medium_sans)
    }

    setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
}

@BindingAdapter("unreadMessageFont")
fun TextView.bindUnreadMessage(unreadCount: Int) {
    if (unreadCount > 0) {
        setTypeface(null, Typeface.BOLD)
        ResourcesCompat.getFont(context, R.font.regular_sans)
        maxLines = 2
        setTextColor(context.getColorFromAttr(R.attr.titleTextColor))
    } else {
        setTypeface(null, Typeface.NORMAL)
        ResourcesCompat.getFont(context, R.font.regular_sans)
        maxLines = 1
        setTextColor(context.getColorFromAttr(R.attr.subTextColor))
    }
}

@BindingAdapter("selectionBackground")
fun View.bindSelectionBackground(isSelected: Boolean) {
    if (isSelected) {
        setBackgroundColor(context.getColorFromAttr(R.attr.itemSelectedColor))
    } else {
        background = null
    }
}

@BindingAdapter("selectedTextColor")
fun TextView.setSelectedTextColor(isSelected: Boolean) {
    val color = if (isSelected) {
        ContextCompat.getColor(context, R.color.white)
    } else {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(
            R.attr.titleTextColor,
            typedValue,
            true
        )
        typedValue.data
    }
    setTextColor(color)
}

@BindingAdapter("passwordBackgroundTint")
fun setBackgroundTint(view: View, isSelected: Boolean?) {
    val context = view.context

    view.backgroundTintList = if (isSelected == true) {
        ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.app_theme_color)
        )
    } else {
        ColorStateList.valueOf(
            context.getColorFromAttr(R.attr.itemBackgroundColor)
        )
    }
}

@BindingAdapter("contactAddress", "searchQuery", requireAll = false)
fun TextView.bindContactWithSearch(
    address: String?,
    searchQuery: String?
) {
    if (address.isNullOrBlank()) {
        text = ""
        return
    }

    val name = ContactNameCache.getName(address)
        ?: ContactNameCache.extractName(context, address).also {
            ContactNameCache.putName(address, it)
        }
        ?: address

    text = if (!searchQuery.isNullOrBlank()) {
        highlightAllMatches(name, searchQuery)
    } else {
        SpannableString(name)
    }
}

private fun TextView.highlightAllMatches(
    original: String,
    query: String
): SpannableString {
    val spannable = SpannableString(original)
    if (query.isBlank()) return spannable

    Regex(Regex.escape(query), RegexOption.IGNORE_CASE)
        .findAll(original)
        .forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(context, R.color.app_theme_color)
                ),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                match.range.first,
                match.range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    return spannable
}
















