package com.ktmb.pts.utilities

import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import androidx.annotation.ColorInt

val Int.dp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val String.asColor: Int @ColorInt get() = Color.parseColor(this)

val String.toUri: Uri get() = Uri.parse(this)