package com.ktmb.pts.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Route(
    @SerializedName("code")
    val code: String,
    @SerializedName("polyline_code")
    val polyline: String,
    @SerializedName("from_station")
    val fromStation: Station,
    @SerializedName("to_station")
    val toStation: Station
): Parcelable