package com.ktmb.pts.base

import android.app.Application
import android.content.ContextWrapper
import com.pixplicity.easyprefs.library.Prefs

class PTS: Application() {

    companion object {
        lateinit var instance: PTS private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize the Prefs class
        Prefs.Builder()
            .setContext(this)
            .setMode(ContextWrapper.MODE_PRIVATE)
            .setPrefsName(packageName)
            .setUseDefaultSharedPreference(true)
            .build()
    }

}