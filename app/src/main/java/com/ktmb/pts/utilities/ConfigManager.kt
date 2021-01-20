package com.ktmb.pts.utilities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ktmb.pts.data.model.ReportType
import com.pixplicity.easyprefs.library.Prefs

object ConfigManager {

    fun saveReportTypes(reportTypes: ArrayList<ReportType>?) {
        if (reportTypes != null) {
            val json = Gson().toJson(reportTypes)
            Prefs.putString(Constants.Config.REPORT_TYPES, json)
        }
    }

    fun getReportTypes(): ArrayList<ReportType>? {
        val json = Prefs.getString(Constants.Config.REPORT_TYPES, "")
        return if (json != "") {
            val type = object : TypeToken<List<ReportType>>() {}.type
            ArrayList(Gson().fromJson<List<ReportType>>(json, type))
        } else {
            null
        }
    }
}