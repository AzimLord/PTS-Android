package com.ktmb.pts.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.akexorcist.googledirection.util.DirectionConverter
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.ktmb.pts.R
import com.ktmb.pts.data.model.*
import com.ktmb.pts.event.NewReportEvent
import com.ktmb.pts.event.UpdateUIEvent
import com.pixplicity.easyprefs.library.Prefs
import org.greenrobot.eventbus.EventBus
import java.lang.Exception

object NavigationManager {

    fun startNavigation() {
        Prefs.putBoolean(Constants.Navigation.IS_NAVIGATION_STARTED, true)
    }

    fun stopNavigation() {
        Prefs.putBoolean(Constants.Navigation.IS_NAVIGATION_STARTED, false)
    }

    fun isNavigationStarted(): Boolean {
        return Prefs.getBoolean(Constants.Navigation.IS_NAVIGATION_STARTED, false)
    }

    fun setTrackLastLocation(lastLocation: LatLng?) {
        if (lastLocation != null) {
            val lastLocationJson = Gson().toJson(lastLocation)
            Prefs.putString(Constants.Navigation.LAST_LOCATION, lastLocationJson)
        }
    }

    fun getTrackLastLocation(): LatLng? {
        val lastLocationJson = Prefs.getString(Constants.Navigation.LAST_LOCATION, "")
        return if (lastLocationJson != "") {
            Gson().fromJson<LatLng>(lastLocationJson, LatLng::class.java)
        } else {
            null
        }
    }

    fun clearTrackLastLocation() {
        Prefs.putString(Constants.Navigation.LAST_LOCATION, "")
    }

    fun setReports(reports: ArrayList<Report>) {
        val reportsJson = Gson().toJson(reports)
        Prefs.putString(Constants.Navigation.REPORTS, reportsJson)
    }

    fun newReport(report: Report) {
        val processReport = processReport(report)
        if (processReport != null) {
            val reportsJson = Prefs.getString(Constants.Navigation.REPORTS, "")
            if (reportsJson != "") {
                val type = object : TypeToken<List<Report>>() {}.type
                val reports = ArrayList(Gson().fromJson<List<Report>>(reportsJson, type))
                reports.add(processReport)
                setReports(reports)
                EventBus.getDefault().post(NewReportEvent(processReport))
            }
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

    fun getReports(trackKey: String): ArrayList<Report>? {
        val reportsJson = Prefs.getString(Constants.Navigation.REPORTS, "")
        return if (reportsJson != "") {
            val type = object : TypeToken<List<Report>>() {}.type
            val reports = ArrayList(Gson().fromJson<List<Report>>(reportsJson, type))
            ArrayList(reports.filter {
                it.trackKey == trackKey
            })
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

    fun setNewBearing(bearing: Float) {
        Prefs.putFloat(Constants.Navigation.LAST_BEARING, bearing)
        Prefs.putBoolean(Constants.Navigation.HAVE_LAST_BEARING, true)
    }

    fun getLastBearing(): Float? {
        val hasLastBearing = Prefs.getBoolean(Constants.Navigation.HAVE_LAST_BEARING, false)
        return if (hasLastBearing) {
            Prefs.getFloat(Constants.Navigation.LAST_BEARING, 0f)
        } else {
            null
        }
    }

    fun clearBearing() {
        Prefs.putFloat(Constants.Navigation.LAST_BEARING, 0f)
        Prefs.putBoolean(Constants.Navigation.HAVE_LAST_BEARING, false)
    }

    fun clearNavigation() {
        stopNavigation()
        clearSpeechReport()
        clearReports()
        clearBearing()
        clearTrackLastLocation()
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
        originalLocation: Location,
        textToSpeech: TextToSpeech? = null
    ): LocationUpdate? {
        val tracks = ConfigManager.getTracks()
        val newPossibleLocation = ArrayList<Location>()
        var newLocationUpdate: LocationUpdate? = null
        var routeCoordinates: ArrayList<LatLng>? = null

        if (tracks == null) {
            return null
        } else {
            tracks.forEach {
                val nearestPoint = findNearestPoint(
                    LatLng(originalLocation.latitude, originalLocation.longitude),
                    parseCoordinate(it.coordinates)
                )
                if (nearestPoint != null) {
                    val location = Location("")
                    location.latitude = nearestPoint.latitude
                    location.longitude = nearestPoint.longitude
                    newPossibleLocation.add(location)
                }
            }
        }

        newPossibleLocation.forEach {
            tracks.forEach { track ->
                val distanceFromOriginalLocation = originalLocation.distanceTo(it)
                val speed = (originalLocation.speed * 3600 / 1000).toInt()

                if (newLocationUpdate == null) {
                    routeCoordinates = parseCoordinate(track.coordinates)
                    newLocationUpdate = LocationUpdate(
                        speed,
                        LatLng(it.latitude, it.longitude),
                        distanceFromOriginalLocation,
                        track.key,
                        track.from,
                        track.to
                    )
                } else {
                    if (PolyUtil.isLocationOnPath(
                            LatLng(it.latitude, it.longitude),
                            parseCoordinate(track.coordinates),
                            false, 0.1
                        ) &&
                        newLocationUpdate!!.distanceFromOriginalLocation > distanceFromOriginalLocation
                    ) {
                        routeCoordinates = parseCoordinate(track.coordinates)
                        newLocationUpdate = LocationUpdate(
                            speed,
                            LatLng(it.latitude, it.longitude),
                            distanceFromOriginalLocation,
                            track.key,
                            track.from,
                            track.to
                        )
                    }
                }
            }
        }

        if (routeCoordinates != null && newLocationUpdate != null) {
            val track = ConfigManager.getTrack(newLocationUpdate!!.trackKey)
            val lastTrackLocation = getTrackLastLocation()
            if (track != null && lastTrackLocation != null) {
                try {
                    val coordinates = parseCoordinate(track.coordinates)
                    val trackIndex = PolyUtil.locationIndexOnPath(
                        newLocationUpdate!!.newLocation,
                        coordinates,
                        true
                    )
                    val lastLocationBearing =
                        getLocationBearing(lastTrackLocation, newLocationUpdate!!.newLocation)
                    val trackBearing =
                        getLocationBearing(coordinates[trackIndex], coordinates[trackIndex + 1])

                    if (lastLocationBearing == 0.0f) {
                        newLocationUpdate!!.status = LocationUpdate.Status.STOPPED
                    } else {
                        LogManager.log(" \nLast Location Bearing: $lastLocationBearing\nTrack Bearing: $trackBearing")
                        newLocationUpdate!!.status = LocationUpdate.Status.MOVING

                        if (trackBearing >= 0) {
                            if (lastLocationBearing > trackBearing - 225 && lastLocationBearing < trackBearing - 135) {
                                newLocationUpdate!!.trackDirection =
                                    LocationUpdate.Direction.REVERSE
                            } else {
                                newLocationUpdate!!.trackDirection =
                                    LocationUpdate.Direction.FORWARD
                            }
                        } else {
                            if (lastLocationBearing < trackBearing + 225 && lastLocationBearing > trackBearing + 135) {
                                newLocationUpdate!!.trackDirection =
                                    LocationUpdate.Direction.REVERSE
                            } else {
                                newLocationUpdate!!.trackDirection =
                                    LocationUpdate.Direction.FORWARD
                            }
                        }
                    }
                } catch (e: ArrayIndexOutOfBoundsException) {
                    // Do nothing
                }
            }

            val reports = getReports(newLocationUpdate!!.trackKey)
            LogManager.log(" \nReport size: ${reports?.size}\nText to speech: $textToSpeech")

            if (reports != null && textToSpeech != null) {
                getNextReport(
                    textToSpeech,
                    routeCoordinates!!,
                    reports,
                    newLocationUpdate!!
                )
            }
        }

        setTrackLastLocation(newLocationUpdate?.newLocation)

        LogManager.log(newLocationUpdate.toString())

        return newLocationUpdate
    }

    fun parseCoordinate(coordinates: ArrayList<Coordinate>): ArrayList<LatLng> {
        val latLng = ArrayList<LatLng>()
        coordinates.forEach {
            latLng.add(LatLng(it.latitude, it.longitude))
        }
        return latLng
    }

    private fun getLocationBearing(fromLatLng: LatLng, toLatLng: LatLng): Float {
        val location1 = Location("Self provider")
        location1.latitude = fromLatLng.latitude
        location1.longitude = fromLatLng.longitude

        val location2 = Location("Self provider")
        location2.latitude = toLatLng.latitude
        location2.longitude = toLatLng.longitude

        return location1.bearingTo(location2)
    }

    fun processReports(
        reports: ArrayList<Report>
    ): ArrayList<Report> {
        val filteredReports = ArrayList<Report>()
        reports.forEach {
            val processReport = processReport(it)
            if (processReport != null) {
                filteredReports.add(processReport)
            }
        }

        filteredReports.sortBy {
            it.mapIndex
        }

        return filteredReports
    }

    private fun processReport(report: Report): Report? {
        val originalLatLng = LatLng(report.latitude, report.longitude)
        val trackCoordinates = ConfigManager.getTrack(report.trackKey)

        if (trackCoordinates != null) {
            val processedLatLng =
                findNearestPoint(
                    LatLng(report.latitude, report.longitude),
                    parseCoordinate(trackCoordinates.coordinates)
                )

            val originalAndProcessedDistance = SphericalUtil.computeDistanceBetween(
                originalLatLng,
                processedLatLng
            )

            if (originalAndProcessedDistance < 1000) {

                val reportPosition = findNearestPoint(
                    LatLng(report.latitude, report.longitude),
                    parseCoordinate(trackCoordinates.coordinates)
                )
                val reportIndex = PolyUtil.locationIndexOnEdgeOrPath(
                    reportPosition,
                    parseCoordinate(trackCoordinates.coordinates),
                    false,
                    false,
                    0.01
                )
                report.mapIndex = reportIndex

                val reverseCoordinates = trackCoordinates.coordinates
                reverseCoordinates.reverse()

                val reportReverseIndex = PolyUtil.locationIndexOnEdgeOrPath(
                    reportPosition,
                    parseCoordinate(reverseCoordinates),
                    false,
                    false,
                    0.01
                )

                report.mapReverseIndex = reportReverseIndex

                return report
            }
        }

        return null
    }

    private fun getNextReport(
        textToSpeech: TextToSpeech,
        routeCoordinates: ArrayList<LatLng>,
        reports: ArrayList<Report>,
        newLocationUpdate: LocationUpdate
    ) {
        if (newLocationUpdate.trackDirection != null) {
            if (newLocationUpdate.trackDirection == LocationUpdate.Direction.REVERSE) {
                routeCoordinates.reverse()
                reports.sortBy {
                    it.mapReverseIndex
                }
            }

            var nearestReport: Report? = null
            var nearestReportIndex = -1
            val currentLocationIndex = PolyUtil.locationIndexOnEdgeOrPath(
                newLocationUpdate.newLocation,
                routeCoordinates,
                false,
                false,
                0.01
            )
            var totalDistance = 0.0

            nearestReport = try {
                if (newLocationUpdate.trackDirection == LocationUpdate.Direction.REVERSE) {
                    reports.first {
                        if (it.mapReverseIndex != null) {
                            it.mapReverseIndex!! >= currentLocationIndex
                        } else {
                            false
                        }
                    }
                } else {
                    reports.first {
                        if (it.mapIndex != null) {
                            it.mapIndex!! >= currentLocationIndex
                        } else {
                            false
                        }
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
                nearestReportIndex =
                    if (newLocationUpdate.trackDirection == LocationUpdate.Direction.REVERSE) {
                        nearestReport.mapReverseIndex!! + 1
                    } else {
                        nearestReport.mapIndex!! + 1
                    }

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
                                    newLocationUpdate.newLocation,
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
                            triggerTextToSpeech(nearestReport, totalDistance, 500, textToSpeech)
                        }
                        totalDistance > 570 && totalDistance <= 1100 -> {
                            triggerTextToSpeech(nearestReport, totalDistance, 1000, textToSpeech)
                        }
                        totalDistance > 1700 && totalDistance <= 2100 -> {
                            triggerTextToSpeech(nearestReport, totalDistance, 2000, textToSpeech)
                        }
                        totalDistance > 2700 && totalDistance <= 3100 -> {
                            triggerTextToSpeech(nearestReport, totalDistance, 3000, textToSpeech)
                        }
                        totalDistance > 3700 && totalDistance <= 4100 -> {
                            triggerTextToSpeech(nearestReport, totalDistance, 4000, textToSpeech)
                        }
                        totalDistance > 4700 && totalDistance <= 5100 -> {
                            triggerTextToSpeech(nearestReport, totalDistance, 5000, textToSpeech)
                        }
                    }
                }

                LogManager.log(
                    " " +
                            "\nReport index: $nearestReportIndex, " +
                            "\nCurrent location index: $currentLocationIndex, " +
                            "\nNum Of Loop: $numOfLoop, " +
                            "\nDistance: $totalDistance"
                )

            }

            if (totalDistance > 0 && totalDistance <= 5000) {
                EventBus.getDefault().post(UpdateUIEvent(newLocationUpdate, nearestReport, totalDistance))
                return
            }
        }

        EventBus.getDefault().post(UpdateUIEvent(newLocationUpdate, null, null))
    }

    private fun triggerTextToSpeech(report: Report, originalDistance: Double, distance: Int, textToSpeech: TextToSpeech) {
        if (!isSpeechAlreadyTrigger(report, distance)) {
            val speech = "${report.reportType.name} reported in ${Utilities.DistanceHelper.speakMeter(originalDistance)}"

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
//        reports.add(Report(3, 1.8591425691047436, 103.4144429810641, ReportType(3, "Flood")))
//        reports.add(
//            Report(
//                2,
//                1.5207286968462848,
//                103.73925055206125,
//                ReportType(1, "Landslide")
//            )
//        )
//        reports.add(
//            Report(
//                1,
//                1.4917206891982524,
//                103.75720931494729,
//                ReportType(2, "Animal Herd")
//            )
//        )
        return reports
    }

}