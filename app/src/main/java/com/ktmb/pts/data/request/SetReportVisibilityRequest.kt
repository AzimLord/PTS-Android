package com.ktmb.pts.data.request

import com.google.gson.annotations.SerializedName

data class SetReportVisibilityRequest(
    @SerializedName("marker_id") val markerId: Int,
    @SerializedName("is_visible") val isVisible: Boolean
)