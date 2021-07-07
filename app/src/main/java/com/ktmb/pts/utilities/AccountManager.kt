package com.ktmb.pts.utilities

import com.ktmb.pts.data.model.User
import com.pixplicity.easyprefs.library.Prefs

object AccountManager {

    private const val AUTH_TOKEN = "auth_token"
    private const val FIREBASE_TOKEN = "firebase_token"
    private const val KTMB_ID = "ktmb_id"
    private const val NAME = "name"

    fun saveAuthToken(token: String) {
        Prefs.putString(AUTH_TOKEN, token)
    }

    fun getAuthToken(): String {
        return Prefs.getString(AUTH_TOKEN, "")
    }

    fun saveUserInfo(user: User) {
        Prefs.putString(KTMB_ID, user.ktmbId)
        Prefs.putString(NAME, user.name)
    }

    fun getUserInfo(): User {
        val ktmbId = Prefs.getString(KTMB_ID, "")
        val name = Prefs.getString(NAME, "")

        return User(ktmbId, name)
    }

    fun isLoggedIn(): Boolean {
        return getAuthToken().isNotEmpty()
    }

    fun saveFirebaseToken(token: String?) {
        Prefs.putString(FIREBASE_TOKEN, token ?: "")
    }

    fun isFirebaseTokenAlreadyStored(token: String): Boolean {
        val storedToken = Prefs.getString(FIREBASE_TOKEN, "")
        return storedToken == token
    }

    fun clear() {
        Prefs.clear()
    }

}