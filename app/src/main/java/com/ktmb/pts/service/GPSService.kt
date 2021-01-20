package com.ktmb.pts.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleService
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.ktmb.pts.R
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.data.model.Route
import com.ktmb.pts.event.NewReportEvent
import com.ktmb.pts.ui.start.view.SplashActivity
import com.ktmb.pts.utilities.Constants
import com.ktmb.pts.utilities.LogManager
import com.ktmb.pts.utilities.NavigationManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*
import kotlin.collections.ArrayList

class GPSService: LifecycleService() {

    private lateinit var locationManager: LocationManager
    private lateinit var textToSpeech: TextToSpeech
    private var currentLocation: LatLng? = null
    private var route: Route? = null
    private var routeCoordinates: ArrayList<LatLng>? = null
    private var reports: ArrayList<Report> = ArrayList()

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ]
    )
    override fun onCreate() {
        super.onCreate()

        route = NavigationManager.getRoute()

        if (route != null) {
            locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener {})

            routeCoordinates = ArrayList(PolyUtil.decode(route!!.polyline))
            if (NavigationManager.getReports() != null) {
                reports = NavigationManager.getReports()!!
            }

            startNavigation()

            val notification = createNotification()
            startForeground(Random().nextInt(), notification)

            LogManager.log("GPSService Started")
            EventBus.getDefault().register(this)
        } else {
            onDestroy()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopNavigation()
        textToSpeech.shutdown()
        EventBus.getDefault().unregister(this)
        LogManager.log("GPSService Stopped")
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ]
    )
    private fun startNavigation() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            Constants.Config.LOCATION_UPDATE_MIN_TIME,
            Constants.Config.LOCATION_UPDATE_MIN_DISTANCE,
            locationCallback
        )
    }

    private fun stopNavigation() {
        locationManager.removeUpdates(locationCallback)
    }

    private fun getNearestPoint() {
        var nearestReportIndex = -1
        val reports = ArrayList<LatLng>()
        val nearestReport = reports.minBy {
            nearestReportIndex = PolyUtil.locationIndexOnEdgeOrPath(
                it,
                routeCoordinates!!,
                false,
                false,
                0.01
            )
            nearestReportIndex
        }
        LogManager.log("$nearestReportIndex")
    }

    private val locationCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (routeCoordinates != null) {
                NavigationManager.navigationLocationUpdate(currentLocation, location, routeCoordinates!!, reports, textToSpeech)
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onProviderDisabled(provider: String?) {

        }
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "PTS"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "PTS",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            SplashActivity.newIntent(this).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            )
            else Notification.Builder(this)

        return builder
            .setContentTitle("PTS")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    @Subscribe
    fun onNewReportReceived(newReportEvent: NewReportEvent) {
        LogManager.log("New Report Received")
        NavigationManager.newReport(newReportEvent.report)
        if (NavigationManager.getReports() != null) {
            reports = NavigationManager.getReports()!!
        }
    }

}