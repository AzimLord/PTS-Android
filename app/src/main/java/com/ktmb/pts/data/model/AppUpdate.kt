package com.ktmb.pts.data.model

import com.google.gson.annotations.SerializedName

data class AppUpdate(
    @SerializedName("latest_version")
    val latestVersion: String,
    @SerializedName("latest_version_code")
    val latestVersionCode: Int = 0,
    @SerializedName("download_url")
    val downloadUrl: String = "",
    @SerializedName("release_notes")
    val release_notes: String = ""
)