package com.ktmb.pts.ui.report.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ktmb.pts.data.model.ReportType

class ReportTypeViewModel(reportType: ReportType): ViewModel() {

    val name = MutableLiveData(reportType.name)

}