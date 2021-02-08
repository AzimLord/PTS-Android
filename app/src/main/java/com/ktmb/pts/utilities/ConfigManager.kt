package com.ktmb.pts.utilities

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ktmb.pts.data.model.ReportType
import com.ktmb.pts.data.model.Track
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

    fun saveTracks(tracks: ArrayList<Track>?) {
        if (tracks != null) {
            val json = Gson().toJson(tracks)
            Prefs.putString(Constants.Config.TRACKS, json)
        }
    }

    fun getTracks(): ArrayList<Track>? {
        val json = Prefs.getString(Constants.Config.TRACKS, "")
        return if (json != "") {
            val type = object : TypeToken<List<Track>>() {}.type
            ArrayList(Gson().fromJson<List<Track>>(json, type))
        } else {
            null
        }
    }

    fun getTrack(key: String?): Track? {
        val tracks = getTracks()
        return tracks?.singleOrNull {
            it.key == key
        }
    }
}