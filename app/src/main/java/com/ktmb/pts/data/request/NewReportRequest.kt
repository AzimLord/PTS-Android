package com.ktmb.pts.data.request

import com.google.gson.annotations.SerializedName

data class NewReportRequest(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("marker_category_id")
    val reportTypeId: Int,
    @SerializedName("track_key")
    val track_key: String
)