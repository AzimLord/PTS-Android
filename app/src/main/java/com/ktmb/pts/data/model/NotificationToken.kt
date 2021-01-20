package com.ktmb.pts.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NotificationToken(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val token: String
): Parcelable