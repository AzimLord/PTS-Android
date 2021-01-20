package com.ktmb.pts.base

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ktmb.pts.R

abstract class BaseViewModel : ViewModel() {

    val progressVisibility = MutableLiveData(View.GONE)
    val errorVisibility = MutableLiveData(View.GONE)
    val errorTitle = MutableLiveData("")
    val errorMessage = MutableLiveData("")
    val errorTryAgainVisibility = MutableLiveData(View.GONE)
    val errorTryAgainMessage = MutableLiveData("")

    fun showProgress() {
        progressVisibility.value = View.VISIBLE
    }

    fun hideProgress() {
        progressVisibility.value = View.GONE
    }

    fun showError(
        errorTitle: String? = null,
        errorMessage: String? = null,
        showTryAgain: Boolean = false,
        tryAgainMessage: String? = null
    ) {
        this.errorTitle.value = errorTitle ?: PTS.instance.getString(R.string.error_default_title)
        this.errorMessage.value = errorMessage ?: PTS.instance.getString(R.string.error_default_message)
        this.errorVisibility.value = View.VISIBLE

        if (showTryAgain) {
            errorTryAgainVisibility.value = View.VISIBLE
            errorTryAgainMessage.value = tryAgainMessage ?: PTS.instance.getString(R.string.label_try_again)
        } else {
            errorTryAgainVisibility.value = View.GONE
        }
    }

    fun hideError() {
        this.errorTitle.value = ""
        this.errorMessage.value = ""
        this.errorVisibility.value = View.GONE
        errorTryAgainVisibility.value = View.GONE
        errorTryAgainMessage.value = ""
    }
}