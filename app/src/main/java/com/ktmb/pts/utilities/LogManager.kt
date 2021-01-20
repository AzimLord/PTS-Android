package com.ktmb.pts.utilities

import android.util.Log
import com.ktmb.pts.BuildConfig

object LogManager {

    fun log(message: String, tag: String = BuildConfig.APPLICATION_ID) {
        Log.d(tag, message)
    }

}