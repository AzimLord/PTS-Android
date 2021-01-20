package com.ktmb.pts.ui.route.viewmodel

import androidx.lifecycle.liveData
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.repository.RouteRepo
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class RouteViewModel: BaseViewModel() {

    private val routeRepo = RouteRepo()

    fun getRoutes() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = routeRepo.getRoutes()
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    emit(Resource.success(response.body(), response))
                } else {
                    emit(Resource.error(response))
                }
            } else {
                emit(Resource.error(response))
            }
        } catch (exception: Exception) {
            emit(Resource.error(exception))
        }
    }


}