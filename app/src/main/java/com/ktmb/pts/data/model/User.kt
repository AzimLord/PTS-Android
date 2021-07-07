package com.ktmb.pts.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("ktmb_id")
    val ktmbId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("token")
    val token: String? = null
)
