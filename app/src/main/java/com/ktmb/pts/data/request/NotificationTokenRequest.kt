package com.ktmb.pts.data.request

import com.google.gson.annotations.SerializedName

data class NotificationTokenRequest(
    @SerializedName("token")
    val token: String
)