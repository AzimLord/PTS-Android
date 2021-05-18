package com.ktmb.pts.event

import com.ktmb.pts.data.model.Report
import com.ktmb.pts.notification.NotificationType

class ReportEvent(
    val report: Report,
    val type: NotificationType = NotificationType.REPORT
)