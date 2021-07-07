package com.ktmb.pts.data.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import com.itkacher.okhttpprofiler.OkHttpProfilerInterceptor
import com.ktmb.pts.BuildConfig
import com.ktmb.pts.data.CrashlyticsInterceptor
import com.ktmb.pts.utilities.AccountManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiProvider {

    private const val baseUrl = "https://luminous-potable-barnowl.gigalixirapp.com/staff/api/v1/"

    private const val timeout = 30L

    //private val token = if (!AccountManager.get().token.isNullOrEmpty()) AccountManager.get().token!! else ""

    private val converterFactory = GsonConverterFactory.create()

    private fun httpClient(): OkHttpClient.Builder? {
        return OkHttpClient().newBuilder()
            .addInterceptor(OkHttpProfilerInterceptor())
            .addInterceptor(CrashlyticsInterceptor())
            .addNetworkInterceptor(StethoInterceptor())
    }

    private val logging = LoggingInterceptor.Builder()
        .setLevel(if (BuildConfig.DEBUG) Level.BASIC else Level.NONE)
        .addHeader("Authorization", AccountManager.getAuthToken())
        .addHeader("Content-Type", "application/json")
        .build()

    private val client = httpClient()!!
        .readTimeout(timeout, TimeUnit.SECONDS)
        .connectTimeout(timeout, TimeUnit.SECONDS)
        .addInterceptor(logging).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(converterFactory)
        .build()

    fun createService(): PTSApi {
        return retrofit.create(PTSApi::class.java)
    }
}