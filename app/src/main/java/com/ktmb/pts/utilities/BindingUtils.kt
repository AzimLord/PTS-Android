package com.ktmb.pts.utilities

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object BindingUtils {

    @JvmStatic
    @BindingAdapter("setImageUrl")
    fun setImageUrl(imageView: ImageView, url: String?) {
        Glide.with(imageView.context).load(url).into(imageView)
    }

}