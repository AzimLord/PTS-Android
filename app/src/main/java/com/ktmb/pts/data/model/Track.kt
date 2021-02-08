package com.ktmb.pts.data.model

import com.google.gson.annotations.SerializedName

data class Track(
    @SerializedName("key")
    val key: String,
    @SerializedName("coordinates")
    val coordinates: ArrayList<Coordinate>
)
