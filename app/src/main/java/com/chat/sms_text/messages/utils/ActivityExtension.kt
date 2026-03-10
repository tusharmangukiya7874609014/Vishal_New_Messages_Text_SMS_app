package com.chat.sms_text.messages.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

fun Context.getDrawableFromAttr(@AttrRes attr: Int): Drawable? {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return AppCompatResources.getDrawable(this, typedValue.resourceId)
}