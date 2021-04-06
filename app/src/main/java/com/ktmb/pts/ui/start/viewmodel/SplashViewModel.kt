package com.ktmb.pts.ui.start.viewmodel

import android.content.Context
import androidx.lifecycle.liveData
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.model.AppUpdate
import com.ktmb.pts.data.repository.AccountRepo
import com.ktmb.pts.data.repository.AppUpdateRepo
import com.ktmb.pts.data.repository.ReportRepo
import com.ktmb.pts.data.repository.RouteRepo
import com.ktmb.pts.data.request.NotificationTokenRequest
import com.ktmb.pts.utilities.ConfigManager
import com.ktmb.pts.utilities.Resource
import com.ktmb.pts.utilities.Utilities
import kotlinx.coroutines.Dispatchers

class SplashViewModel : BaseViewModel() {

    private val reportRepo = ReportRepo()
    private val accountRepo = AccountRepo()
    private val routeRepo = RouteRepo()
    private val appUpdateRepo = AppUpdateRepo()

    fun checkAppUpdate() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = appUpdateRepo.checkAppUpdate()
            if (response.isSuccessful) {
                if (response.body() != null) {
                    emit(Resource.success(response.body()!!, response))
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

    fun getReportTypes() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = reportRepo.getReportTypes()
            if (response.isSuccessful) {
                if (response.body() != null) {
                    ConfigManager.saveReportTypes(response.body())
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

    fun getTracks() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = routeRepo.getTracks()
            if (response.isSuccessful) {
                if (response.code() == 200) {
                    if (response.body() != null) {
                        ConfigManager.saveTracks(response.body())
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