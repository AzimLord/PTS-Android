package com.ktmb.pts.data.repository

import com.ktmb.pts.data.api.ApiProvider

class AppUpdateRepo {

    private var client = ApiProvider.createService()

    suspend fun checkAppUpdate() = client.checkAppUpdate()

}