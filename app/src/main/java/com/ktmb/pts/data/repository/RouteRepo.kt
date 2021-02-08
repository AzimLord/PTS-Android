package com.ktmb.pts.data.repository

import com.ktmb.pts.data.api.ApiProvider

class RouteRepo {

    private var client = ApiProvider.createService()

    suspend fun getRoutes() = client.getRoutes()

    suspend fun getTracks() = client.getTracks()

}