package com.ktmb.pts.ui.main.viewmodel

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.google.android.gms.maps.model.LatLng
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.base.PTS
import com.ktmb.pts.data.repository.ReportRepo
import com.ktmb.pts.utilities.NavigationManager
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class MainViewModel: BaseViewModel() {

    private val reportRepo = ReportRepo()

    val gpsStatusVisibility = MutableLiveData(View.VISIBLE)
    val reportVisibility = MutableLiveData(View.GONE)
    val reportConfirmationVisibility = MutableLiveData(View.GONE)
    val recenterVisibility = MutableLiveData(View.GONE)
    val navigationBtnVisibility = MutableLiveData(View.GONE)
    val nonNavigationBtnVisibility = MutableLiveData(View.VISIBLE)
    val routeNameVisibility = MutableLiveData(View.GONE)
    val primaryButtonText = MutableLiveData(PTS.instance.getString(R.string.label_navigation_start))
    val speed = MutableLiveData("")
    val reportID = MutableLiveData("")
    val reportImage = MutableLiveData("")
    val reportName = MutableLiveData("")
    val reportDistance = MutableLiveData("")
    val routeName = MutableLiveData("")

    fun getReports() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = reportRepo.getReports()
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        val filteredReport = NavigationManager.processReports(response.body()!!)
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

    fun setReportVisibility(reportId: Int, isVisible: Boolean) = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = reportRepo.setReportVisibility(reportId, isVisible)
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    when {
                        response.body() != null -> {
                            // TODO: Update report list and reported marker
                            emit(Resource.success(response.body(), response))
                        }
                        response.code() == 404 -> {
                            emit(Resource.success(null, response))
                        }
                        else -> {
                            emit(Resource.error(response))
                        }
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