package com.ktmb.pts.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Report(
    @SerializedName("id")
    val id: Int,
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("category")
    val reportType: ReportType,
    @SerializedName("track_key")
    val trackKey: String? = null,
    @SerializedName("custom_tts")
    val customTTS: String? = null,
    @SerializedName("mapIndex")
    var mapIndex: Int? = null,
    @SerializedName("mapReverseIndex")
    var mapReverseIndex: Int? = null
): Parcelable