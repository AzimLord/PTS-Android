package com.ktmb.pts.ui.start.viewmodel

import androidx.lifecycle.liveData
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.repository.AccountRepo
import com.ktmb.pts.data.repository.ReportRepo
import com.ktmb.pts.data.request.NotificationTokenRequest
import com.ktmb.pts.utilities.ConfigManager
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class SplashViewModel: BaseViewModel() {

    private val reportRepo = ReportRepo()
    private val accountRepo = AccountRepo()

    fun getReportTypes() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = reportRepo.getReportTypes()
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        ConfigManager.saveReportTypes(response.body())
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
        } catch (exception: Exception) {
            emit(Resource.error(exception))
        }
    }

    fun saveFirebaseToken(token: String) = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = accountRepo.saveNotificationToken(NotificationTokenRequest(token))
            if (response.isSuccessful) {
                if (response.code() == 200) {
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
        } catch (exception: Exception) {
            emit(Resource.error(exception))
        }
    }
}