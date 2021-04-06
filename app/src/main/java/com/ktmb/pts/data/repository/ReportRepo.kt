package com.ktmb.pts.data.repository

import com.ktmb.pts.data.api.ApiProvider
import com.ktmb.pts.data.request.NewReportRequest
import com.ktmb.pts.data.request.SetReportVisibilityRequest

class ReportRepo {

    private var client = ApiProvider.createService()

    suspend fun getReportTypes() = client.getReportCategories()

    suspend fun getReports() = client.getReports()

    suspend fun createReport(request: NewReportRequest) = client.createReport(request)

    suspend fun setReportVisibility(reportId: Int, isVisible: Boolean) = client.setMarkerVisibility(
        SetReportVisibilityRequest(reportId, isVisible)
    )

}