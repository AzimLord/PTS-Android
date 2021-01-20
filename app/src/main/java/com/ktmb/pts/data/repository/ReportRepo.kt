package com.ktmb.pts.data.repository

import com.ktmb.pts.data.api.ApiProvider
import com.ktmb.pts.data.request.NewReportRequest

class ReportRepo {

    private var client = ApiProvider.createService()

    suspend fun getReportTypes() = client.getReportCategories()

    suspend fun getReports() = client.getReports()

    suspend fun createReport(request: NewReportRequest) = client.createReport(request)

}