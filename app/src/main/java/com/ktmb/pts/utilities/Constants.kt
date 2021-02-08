package com.ktmb.pts.utilities

object Constants {

    object Config {

        const val LOCATION_UPDATE_MIN_TIME = 1000L      // In milliseconds
        const val LOCATION_UPDATE_MIN_DISTANCE = 0f    // In meters
        const val LOCATION_MAP_ZOOM = 17f
        const val REPORT_TYPES = "report_types"
        const val TRACKS = "tracks"

    }

    object Navigation {
        const val IS_NAVIGATION_STARTED = "is_navigation_started"
        const val LAST_LOCATION = "last_location"
        const val REPORTS = "currentReports"
        const val LAST_SPEECH_REPORT_ID = "last_speech_report_id"
        const val LAST_SPEECH_DISTANCE = "last_speech_distance"
        const val HAVE_LAST_BEARING = "have_last_bearing"
        const val LAST_BEARING = "last_bearing"

    }

    object Event {
        const val RETRY_SPLASH = "retry_splash"
    }

}