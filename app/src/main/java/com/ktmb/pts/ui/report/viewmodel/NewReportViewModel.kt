package com.ktmb.pts.ui.report.viewmodel

import androidx.lifecycle.liveData
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.repository.ReportRepo
import com.ktmb.pts.data.request.NewReportRequest
import com.ktmb.pts.utilities.NavigationManager
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class NewReportViewModel: BaseViewModel() {

    private val reportRepo = ReportRepo()

    fun newReport(request: NewReportRequest) = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = reportRepo.createReport(request)
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        if (response.body() != null) {
                            emit(Resource.success(response.body(), response))
                        } else {
                            emit(Resource.error(response))
                        }
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