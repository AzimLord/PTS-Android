package com.ktmb.pts.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.speech.tts.TextToSpeech
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.ktmb.pts.data.model.MapUpdate
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.data.model.ReportType
import com.ktmb.pts.data.model.Route
import com.pixplicity.easyprefs.library.Prefs
import java.lang.Exception

object NavigationManager {

    fun setRoute(route: Route) {
        val routeJson = Gson().toJson(route)
        Prefs.putString(Constants.Navigation.ROUTE, routeJson)
    }

    fun getRoute(): Route? {
        val routeJson = Prefs.getString(Constants.Navigation.ROUTE, "")
        return if (routeJson != "") {
            Gson().fromJson<Route>(routeJson, Route::class.java)
        } else {
            null
        }
    }

    private fun clearRoute() {
        Prefs.putString(Constants.Navigation.ROUTE, "")
    }

    fun setReports(reports: ArrayList<Report>) {
        val reportsJson = Gson().toJson(reports)
        Prefs.putString(Constants.Navigation.REPORTS, reportsJson)
    }

    fun newReport(report: Report) {
        val reportsJson = Prefs.getString(Constants.Navigation.REPORTS, "")
        val currentRoute = getRoute()
        if (reportsJson != "" && currentRoute != null) {
            val type = object : TypeToken<List<Report>>() {}.type
            val reports = ArrayList(Gson().fromJson<List<Report>>(reportsJson, type))
            reports.add(report)
            val routeCoordinates = ArrayList(PolyUtil.decode(currentRoute.polyline))
            setReports(processReports(reports, routeCoordinates))
        }
    }

    fun getReports(): ArrayList<Report>? {
        val reportsJson = Prefs.getString(Constants.Navigation.REPORTS, "")
        return if (reportsJson != "") {
            val type = object : TypeToken<List<Report>>() {}.type
            ArrayList(Gson().fromJson<List<Report>>(reportsJson, type))
        } else {
            null
        }
    }

    private fun clearReports() {
        Prefs.putString(Constants.Navigation.REPORTS, "")
    }

    private fun setNewSpeechReport(report: Report, distance: Int) {
        Prefs.putInt(Constants.Navigation.LAST_SPEECH_REPORT_ID, report.id)
        Prefs.putInt(Constants.Navigation.LAST_SPEECH_DISTANCE, distance)
    }

    private fun isSpeechAlreadyTrigger(report: Report, distance: Int): Boolean {
        val reportId = Prefs.getInt(Constants.Navigation.LAST_SPEECH_REPORT_ID, -1)
        val reportDistance = Prefs.getInt(Constants.Navigation.LAST_SPEECH_DISTANCE, -1)
        return if (reportId == -1 && reportDistance == -1) {
            setNewSpeechReport(report, distance)
            false
        } else {
            if (reportId == report.id && distance == reportDistance) {
                true
            } else {
                setNewSpeechReport(report, distance)
                false
            }
        }
    }

    private fun clearSpeechReport() {
        Prefs.putInt(Constants.Navigation.LAST_SPEECH_REPORT_ID, -1)
        Prefs.putInt(Constants.Navigation.LAST_SPEECH_DISTANCE, -1)
    }

//    fun setNextReport(report: Report) {
//        val routeJson = Gson().toJson(report)
//        Prefs.putString(Constants.Navigation.NEXT_REPORT, routeJson)
//    }
//
//    fun getNextReport(): Report? {
//        val nextReportJson = Prefs.getString(Constants.Navigation.NEXT_REPORT, "")
//        return if (nextReportJson != "") {
//            Gson().fromJson<Report>(nextReportJson, Report::class.java)
//        } else {
//            null
//        }
//    }
//
//    fun clearNextReport() {
//        Prefs.putString(Constants.Navigation.NEXT_REPORT, "")
//    }

    fun clearNavigation() {
        clearRoute()
        clearSpeechReport()
        clearReports()
    }

    // https://stackoverflow.com/questions/36104809/find-the-closest-point-on-polygon-to-user-location/36105498#36105498
    fun findNearestPoint(gpsPos: LatLng, target: List<LatLng>): LatLng? {
        var distance = -1.0
        var minimumDistancePoint: LatLng? = gpsPos
//        if (test == null || target == null) {
//            return minimumDistancePoint
//        }
        for (i in target.indices) {
            val point = target[i]
            var segmentPoint = i + 1
            if (segmentPoint >= target.size) {
                segmentPoint = 0
            }
            val currentDistance =
                PolyUtil.distanceToLine(gpsPos, point, target[segmentPoint])
            if (distance == -1.0 || currentDistance < distance) {
                distance = currentDistance
                minimumDistancePoint = findNearestPoint(gpsPos, point, target[segmentPoint])
            }
        }
        return minimumDistancePoint
    }

    private fun findNearestPoint(p: LatLng, start: LatLng, end: LatLng): LatLng? {
        if (start == end) {
            return start
        }
        val s0lat = Math.toRadians(p.latitude)
        val s0lng = Math.toRadians(p.longitude)
        val s1lat = Math.toRadians(start.latitude)
        val s1lng = Math.toRadians(start.longitude)
        val s2lat = Math.toRadians(end.latitude)
        val s2lng = Math.toRadians(end.longitude)
        val s2s1lat = s2lat - s1lat
        val s2s1lng = s2lng - s1lng
        val u = (((s0lat - s1lat) * s2s1lat + (s0lng - s1lng) * s2s1lng)
                / (s2s1lat * s2s1lat + s2s1lng * s2s1lng))
        if (u <= 0) {
            return start
        }
        return if (u >= 1) {
            end
        } else LatLng(
            start.latitude + u * (end.latitude - start.latitude),
            start.longitude + u * (end.longitude - start.longitude)
        )
    }

    fun navigationLocationUpdate(
        currentLocation: LatLng?,
        newLocation: Location,
        routeCoordinates: ArrayList<LatLng>,
        reports: ArrayList<Report>,
        textToSpeech: TextToSpeech
    ): MapUpdate? {
        val newCurrentLocation =
            findNearestPoint(LatLng(newLocation.latitude, newLocation.longitude), routeCoordinates)

        return if (newCurrentLocation == null && !PolyUtil.isLocationOnPath(
                newCurrentLocation,
                routeCoordinates,
                false,
                0.1
            )
        ) {
            LogManager.log("newCurrentLocation = $newCurrentLocation")
            LogManager.log("New current location is null & new location is not on path")

            null
        } else {
            //LogManager.log("${currentLocation!!.latitude} ${currentLocation!!.longitude}")

            val speed = (newLocation.speed * 3600 / 1000).toInt()
            getNextReport(textToSpeech, routeCoordinates, reports, newCurrentLocation!!)

            // TODO: Send location to backend

            MapUpdate(speed, newCurrentLocation)
        }
    }

    fun processReports(
        reports: ArrayList<Report>,
        routeCoordinates: ArrayList<LatLng>
    ): ArrayList<Report> {
        val filteredReports = ArrayList<Report>()
        reports.forEach {
            val originalLatLng = LatLng(it.latitude, it.longitude)
            val processedLatLng =
                findNearestPoint(LatLng(it.latitude, it.longitude), routeCoordinates)

            val originalAndProcessedDistance = SphericalUtil.computeDistanceBetween(
                originalLatLng,
                processedLatLng
            )

            if (originalAndProcessedDistance < 1000) {

                val reportPosition = findNearestPoint(
                    LatLng(it.latitude, it.longitude),
                    routeCoordinates
                )
                val reportIndex = PolyUtil.locationIndexOnEdgeOrPath(
                    reportPosition,
                    routeCoordinates,
                    false,
                    false,
                    0.01
                )
                it.mapIndex = reportIndex

                filteredReports.add(it)
            }
        }

        filteredReports.sortBy {
            it.mapIndex
        }

        return filteredReports
    }

    private fun getNextReport(
        textToSpeech: TextToSpeech,
        routeCoordinates: ArrayList<LatLng>,
        reports: ArrayList<Report>,
        currentLocation: LatLng
    ) {
        var nearestReport: Report? = null
        var nearestReportIndex = -1
        val currentLocationIndex = PolyUtil.locationIndexOnEdgeOrPath(
            currentLocation,
            routeCoordinates,
            false,
            false,
            0.01
        )
        var totalDistance = 0.0

        nearestReport = try {
            reports.first {
                if (it.mapIndex != null) {
                    it.mapIndex!! >= currentLocationIndex
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            null
        }

        if (nearestReport != null) {
            val reportPosition = findNearestPoint(
                LatLng(nearestReport.latitude, nearestReport.longitude),
                routeCoordinates
            )
            nearestReportIndex = nearestReport.mapIndex!! + 1

            val numOfLoop = nearestReportIndex - currentLocationIndex

            if (numOfLoop < 150 && nearestReportIndex != -1 && currentLocationIndex != -1) {
                for (i in currentLocationIndex until nearestReportIndex) {
                    totalDistance += when (i) {
                        nearestReportIndex -> {
                            if (reportPosition != null) {
                                SphericalUtil.computeDistanceBetween(
                                    routeCoordinates[i],
                                    LatLng(reportPosition.latitude, reportPosition.longitude)
                                )
                            } else {
                                SphericalUtil.computeDistanceBetween(
                                    routeCoordinates[i],
                                    LatLng(nearestReport.latitude, nearestReport.longitude)
                                )
                            }
                        }
                        currentLocationIndex -> {
                            SphericalUtil.computeDistanceBetween(
                                currentLocation,
                                routeCoordinates[i + 1]
                            )
                        }
                        else -> {
                            SphericalUtil.computeDistanceBetween(
                                routeCoordinates[i],
                                routeCoordinates[i + 1]
                            )
                        }
                    }
                }

                when {
                    totalDistance > 0 && totalDistance <= 510 -> {
                        triggerTextToSpeech(nearestReport, 500, textToSpeech)
                    }
                    totalDistance > 570 && totalDistance <= 1100 -> {
                        triggerTextToSpeech(nearestReport, 1000, textToSpeech)
                    }
                    totalDistance > 1700 && totalDistance <= 2100 -> {
                        triggerTextToSpeech(nearestReport, 2000, textToSpeech)
                    }
                    totalDistance > 2700 && totalDistance <= 3100 -> {
                        triggerTextToSpeech(nearestReport, 3000, textToSpeech)
                    }
                    totalDistance > 3700 && totalDistance <= 4100 -> {
                        triggerTextToSpeech(nearestReport, 4000, textToSpeech)
                    }
                    totalDistance > 4700 && totalDistance <= 5100 -> {
                        triggerTextToSpeech(nearestReport, 5000, textToSpeech)
                    }
                }
            }

            LogManager.log(
                "" +
                        "Report index: $nearestReportIndex, " +
                        "Current location index: $currentLocationIndex, " +
                        "Num Of Loop: $numOfLoop, " +
                        "Distance: $totalDistance"
            )
        }
    }

    private fun triggerTextToSpeech(report: Report, distance: Int, textToSpeech: TextToSpeech) {
        if (!isSpeechAlreadyTrigger(report, distance)) {

            val speech = if (distance >= 1000) {
                val speechDistance = distance / 1000
                if (speechDistance > 1) {
                    "${report.reportType.name} reported in $speechDistance kilometers"
                } else {
                    "${report.reportType.name} reported in $speechDistance kilometer"
                }
            } else {
                "${report.reportType.name} reported in $distance meters"
            }

            textToSpeech.speak(
                speech,
                TextToSpeech.QUEUE_FLUSH,
                null
            )
        }
    }

    fun isLocationPermissionGranted(context: Context): Boolean {
        return (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                == PackageManager.PERMISSION_GRANTED)
    }

    fun getMockReports(): ArrayList<Report> {
        val reports = ArrayList<Report>()
        reports.add(Report(3, 1.8591425691047436, 103.4144429810641, ReportType(3, "Flood")))
        reports.add(
            Report(
                2,
                1.5207286968462848,
                103.73925055206125,
                ReportType(1, "Landslide")
            )
        )
        reports.add(
            Report(
                1,
                1.4917206891982524,
                103.75720931494729,
                ReportType(2, "Animal Herd")
            )
        )
        return reports
    }

}