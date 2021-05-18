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
import android.speech.tts.TextToSpeech
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.ktmb.pts.R
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.event.ReportEvent
import com.ktmb.pts.ui.main.view.MainActivity
import com.ktmb.pts.utilities.Constants
import com.ktmb.pts.utilities.LogManager
import com.ktmb.pts.utilities.NavigationManager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.*
import kotlin.collections.ArrayList


class GPSService : LifecycleService() {

    private lateinit var locationManager: LocationManager
    private lateinit var textToSpeech: TextToSpeech
    private var reports: ArrayList<Report> = ArrayList()

    companion object {
        const val ACTION_STOP_SERVICE = "action_stop_service"
    }

    @RequiresPermission(
        allOf = [
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ]
    )
    override fun onCreate() {
        super.onCreate()
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener {})

        startLocationUpdate()

        val notification = createNotification()
        startForeground(Random().nextInt(), notification)

        LogManager.log("GPSService Started")
        EventBus.getDefault().register(this)

        if (NavigationManager.getReports() != null) {
            reports = NavigationManager.getReports()!!
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdate()
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
    private fun startLocationUpdate() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            Constants.Config.LOCATION_UPDATE_MIN_TIME,
            Constants.Config.LOCATION_UPDATE_MIN_DISTANCE,
            locationCallback
        )
    }

    private fun stopLocationUpdate() {
        locationManager.removeUpdates(locationCallback)
    }

    private val locationCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            NavigationManager.navigationLocationUpdate(this@GPSService, location, textToSpeech)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

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
            MainActivity.newIntent(this).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            }

        val builder: Notification.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                this,
                notificationChannelId
            )
            else Notification.Builder(this)

        val stopSelf = Intent(this, GPSService::class.java)
        stopSelf.action = ACTION_STOP_SERVICE
        val stopServicePendingIntent = PendingIntent
            .getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT)

        return builder
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Running. Tap to open")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))
            .setPriority(Notification.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_close,
                getString(R.string.label_service_switch_off),
                stopServicePendingIntent
            )
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && ACTION_STOP_SERVICE == intent.action) {
            NavigationManager.clearNavigation()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY;
        }

        return super.onStartCommand(intent, flags, startId)
    }

    @Subscribe
    fun onNewReportReceived(reportEvent: ReportEvent) {
        LogManager.log("New Report Received")
        if (NavigationManager.getReports() != null) {
            reports = NavigationManager.getReports()!!
        }
    }

}