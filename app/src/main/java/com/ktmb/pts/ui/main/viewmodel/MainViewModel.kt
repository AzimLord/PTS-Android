package com.ktmb.pts.ui.main.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.google.android.gms.maps.model.LatLng
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.repository.ReportRepo
import com.ktmb.pts.utilities.NavigationManager
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class MainViewModel: BaseViewModel() {

    var gpsIsAvailable = MutableLiveData(false)
    private val reportRepo = ReportRepo()

    fun getReports(routeCoordinates: ArrayList<LatLng>) = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = reportRepo.getReports()
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        val filteredReport = NavigationManager.processReports(response.body()!!, routeCoordinates)
                        emit(Resource.success(filteredReport, response))
                    } else {
                        emit(Resource.error(response))
                    }
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