package com.ktmb.pts.utilities

import retrofit2.Response
import java.lang.Exception
import com.ktmb.pts.utilities.Status.SUCCESS
import com.ktmb.pts.utilities.Status.ERROR
import com.ktmb.pts.utilities.Status.LOADING

data class Resource<out T>(
    val status: Status,
    val data: T?,
    val message: String = "",
    val response: Response<*>? = null) {

    companion object {
        fun <T> loading(data: T? = null): Resource<T> =
            Resource(status = LOADING, data = data)

        fun <T> success(data: T? = null, response: Response<*>? = null): Resource<T> =
            Resource(status = SUCCESS, data = data, response = response)

        fun <T> error(message: String, data: T? = null): Resource<T> =
            Resource(status = ERROR, data = data, message = message)

        fun <T> error(response: Response<*>, data: T? = null): Resource<T> {
            return Resource(status = ERROR, data = data, message = ErrorManager.getErrorMessage(response))
        }

        fun <T> error(exception: Exception, data: T? = null): Resource<T> =
            Resource(status = ERROR, data = data, message = ErrorManager.getErrorMessage(exception))
    }

}