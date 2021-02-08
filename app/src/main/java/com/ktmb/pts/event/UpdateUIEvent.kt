package com.ktmb.pts.event

import com.ktmb.pts.data.model.LocationUpdate
import com.ktmb.pts.data.model.Report

data class UpdateUIEvent(
    val locationUpdate: LocationUpdate,
    val upcomingReport: Report?,
    val upcomingReportDistance: Double?
)