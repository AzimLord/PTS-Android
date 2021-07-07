package com.ktmb.pts.ui.credentials.viewmodel

import androidx.lifecycle.liveData
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.repository.AccountRepo
import com.ktmb.pts.data.request.LoginRequest
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class LoginViewModel: BaseViewModel() {

    private val accountRepo = AccountRepo()

    fun login(ktmbId: String, password: String) = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = accountRepo.login(LoginRequest(ktmbId, password))
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
}