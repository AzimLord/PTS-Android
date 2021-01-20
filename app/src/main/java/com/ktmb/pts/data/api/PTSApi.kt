package com.ktmb.pts.data.api

import com.ktmb.pts.data.model.NotificationToken
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.data.model.ReportType
import com.ktmb.pts.data.model.Route
import com.ktmb.pts.data.request.NewReportRequest
import com.ktmb.pts.data.request.NotificationTokenRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PTSApi {

    @GET("routes")
    suspend fun getRoutes(): Response<ArrayList<Route>>

    @GET("categories/marker")
    suspend fun getReportCategories(): Response<ArrayList<ReportType>>

    @GET("markers")
    suspend fun getReports(): Response<ArrayList<Report>>

    @POST("markers")
    suspend fun createReport(@Body request: NewReportRequest): Response<Report>

    @POST("notification_token")
    fun saveNotificationTokenCall(@Body request: NotificationTokenRequest): Call<NotificationToken>

    @POST("notification_token")
    suspend fun saveNotificationToken(@Body request: NotificationTokenRequest): Response<NotificationToken>

}