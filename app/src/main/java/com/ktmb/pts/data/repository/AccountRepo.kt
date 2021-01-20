package com.ktmb.pts.data.repository

import com.ktmb.pts.data.api.ApiProvider
import com.ktmb.pts.data.request.NotificationTokenRequest

class AccountRepo {

    private var client = ApiProvider.createService()

    fun saveNotificationTokenCall(request: NotificationTokenRequest) = client.saveNotificationTokenCall(request)

    suspend fun saveNotificationToken(request: NotificationTokenRequest) = client.saveNotificationToken(request)
}