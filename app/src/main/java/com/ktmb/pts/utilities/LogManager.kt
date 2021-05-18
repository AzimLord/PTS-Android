package com.ktmb.pts.utilities

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ktmb.pts.BuildConfig
import java.lang.Exception

object LogManager {

    fun log(exception: Exception, tag: String = BuildConfig.APPLICATION_ID, sendToCrashlytics: Boolean = false) {
        val message = when {
            exception.message != null -> exception.toString()
            exception.localizedMessage != null -> exception.localizedMessage!!
            else -> exception.toString()
        }

        Log.e(tag, message)

        if (sendToCrashlytics) {
            FirebaseCrashlytics.getInstance().setCustomKey(tag, message)
            FirebaseCrashlytics.getInstance().log(message)
        }
    }

    fun log(message: String, tag: String = BuildConfig.APPLICATION_ID, sendToCrashlytics: Boolean = false) {
        Log.d(tag, message)

        if (sendToCrashlytics) {
            FirebaseCrashlytics.getInstance().setCustomKey(tag, message)
            FirebaseCrashlytics.getInstance().log(message)
        }
    }

}