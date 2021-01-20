package com.ktmb.pts.utilities

object Constants {

    object Config {

        const val LOCATION_UPDATE_MIN_TIME = 1000L      // In milliseconds
        const val LOCATION_UPDATE_MIN_DISTANCE = 0f    // In meters
        const val LOCATION_MAP_ZOOM = 16f
        const val REPORT_TYPES = "report_types"

    }

    object Navigation {

        const val ROUTE = "current_route"
        const val REPORTS = "currentReports"
        const val LAST_SPEECH_REPORT_ID = "last_speech_report_id"
        const val LAST_SPEECH_DISTANCE = "last_speech_distance"

    }

    object Event {
        const val RETRY_SPLASH = "retry_splash"
    }

}