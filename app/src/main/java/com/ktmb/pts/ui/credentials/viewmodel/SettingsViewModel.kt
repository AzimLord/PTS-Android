package com.ktmb.pts.ui.credentials.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import com.ktmb.pts.base.BaseViewModel
import com.ktmb.pts.data.repository.AccountRepo
import com.ktmb.pts.utilities.AccountManager
import com.ktmb.pts.utilities.Resource
import kotlinx.coroutines.Dispatchers

class SettingsViewModel: BaseViewModel() {

    private val accountRepo = AccountRepo()

    val ktmbId = MutableLiveData(AccountManager.getUserInfo().ktmbId)
    val name = MutableLiveData(AccountManager.getUserInfo().name)

    fun logout() = liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val response = accountRepo.logout()
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