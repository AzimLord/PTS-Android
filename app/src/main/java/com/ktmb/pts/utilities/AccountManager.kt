package com.ktmb.pts.utilities

import com.pixplicity.easyprefs.library.Prefs

object AccountManager {

    private const val FIREBASE_TOKEN = "firebase_token"

    fun saveFirebaseToken(token: String?) {
        Prefs.putString(FIREBASE_TOKEN, token ?: "")
    }

    fun isFirebaseTokenAlreadyStored(token: String): Boolean {
        val storedToken = Prefs.getString(FIREBASE_TOKEN, "")
        return storedToken == token
    }

}