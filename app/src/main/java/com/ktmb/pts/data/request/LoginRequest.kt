package com.ktmb.pts.data.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("ktmb_id")
    val ktmbId: String,
    @SerializedName("password")
    val password: String
)