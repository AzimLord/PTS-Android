package com.ktmb.pts.utilities

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ktmb.pts.BuildConfig

object LogManager {

    fun log(message: String, tag: String = BuildConfig.APPLICATION_ID, sendToCrashlytics: Boolean = false) {
        Log.d(tag, message)

        if (sendToCrashlytics) {
            FirebaseCrashlytics.getInstance().setCustomKey(tag, message)
            FirebaseCrashlytics.getInstance().log(message)
        }
    }

}