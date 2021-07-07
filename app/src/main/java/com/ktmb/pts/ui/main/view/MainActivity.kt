package com.ktmb.pts.ui.main.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.StrictMode
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.akexorcist.googledirection.util.DirectionConverter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.data.model.LocationUpdate
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.data.model.Track
import com.ktmb.pts.databinding.ActivityMainBinding
import com.ktmb.pts.event.ReportEvent
import com.ktmb.pts.event.UpdateUIEvent
import com.ktmb.pts.notification.NotificationType
import com.ktmb.pts.service.GPSService
import com.ktmb.pts.ui.credentials.view.SettingsActivity
import com.ktmb.pts.ui.main.viewmodel.MainViewModel
import com.ktmb.pts.ui.report.view.NewReportActivity
import com.ktmb.pts.utilities.*
import com.ktmb.pts.utilities.Utilities.MapHelper.getBitmapFromLink
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.jetbrains.anko.activityUiThread
import org.jetbrains.anko.doAsync
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private lateinit var locationManager: LocationManager
    private lateinit var textToSpeech: TextToSpeech
    private var googleMap: GoogleMap? = null
    private var currentLocation: LocationUpdate? = null
    private var currentLocationMarker: Marker? = null
    private var reports = ArrayList<Report>()
    private var reportMarkers = ArrayList<Marker>()
    private var isFollowLocation = true
    private var navigationStarted = false
    private var mapCameraAnimationInProgress = false

    private var tracks: ArrayList<Track>? = null
    private var trackPolylines = ArrayList<Polyline>()

    companion object {
        private const val REQUEST_LOCATION_ACCESS = 1000
        private const val REQUEST_ROUTE = 1001
        private const val REQUEST_NEW_REPORT = 1002

        fun newIntent(context: Context): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )

        viewModel = ViewModelProvider.NewInstanceFactory().create(MainViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        textToSpeech = TextToSpeech(this) {}
        textToSpeech.setPitch(0.85f)

        val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        (supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment).getMapAsync { googleMap ->
            this.googleMap = googleMap
            this.googleMap!!.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.google_map_light
                )
            )

            binding.vRoute.run {
                googleMap?.setPadding(16.px, 24.px, 0, this.height + 24.px)
            }

            this.googleMap!!.setOnCameraMoveStartedListener(onMapCameraMoveStartedListener)

            init()
        }
    }

    private fun init() {
        displayTracks()

        navigationStarted = NavigationManager.isNavigationStarted()

        if (navigationStarted) {
            currentLocationMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
            if (NavigationManager.getLastBearing() != null) {
                currentLocationMarker?.rotation = NavigationManager.getLastBearing()!!
            }
            binding.btnStart.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
            viewModel.primaryButtonText.value = getString(R.string.label_navigation_stop)

            viewModel.navigationBtnVisibility.value = View.VISIBLE
            viewModel.nonNavigationBtnVisibility.value = View.GONE

            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
            EventBus.getDefault().register(this)
            populateReports()
            NavigationManager.startNavigation()
        }

        binding.btnStart.setOnClickListener {
            if (navigationStarted) {
                dialog = DialogManager.showConfirmationDialog(
                    context = this,
                    title = getString(R.string.label_navigation_stop),
                    message = getString(R.string.dialog_message_navigation_stop),
                    positiveAction = View.OnClickListener {
                        navigationStarted = false
                        currentLocationMarker?.setIcon(Utilities.BitmapHelper.resToBitmap(R.drawable.ic_user_location))
                        binding.btnStart.setBackgroundColor(
                            ContextCompat.getColor(
                                this,
                                R.color.green
                            )
                        )
                        viewModel.primaryButtonText.value =
                            getString(R.string.label_navigation_start)
                        dialog?.dismiss()
                        viewModel.routeNameVisibility.value = View.GONE
                        viewModel.navigationBtnVisibility.value = View.GONE
                        viewModel.nonNavigationBtnVisibility.value = View.VISIBLE
                        EventBus.getDefault().unregister(this)
                        clearReports()
                        NavigationManager.clearNavigation()
                    })
            } else {
                navigationStarted = true
                currentLocationMarker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
                if (NavigationManager.getLastBearing() != null) {
                    currentLocationMarker?.rotation = NavigationManager.getLastBearing()!!
                }
                binding.btnStart.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                viewModel.primaryButtonText.value = getString(R.string.label_navigation_stop)

                viewModel.navigationBtnVisibility.value = View.VISIBLE
                viewModel.nonNavigationBtnVisibility.value = View.GONE

                EventBus.getDefault().register(this)
                populateReports()
                NavigationManager.startNavigation()
            }
        }
    }

    private fun displayTracks() {
        tracks = ConfigManager.getTracks()

        trackPolylines.forEach {
            it.remove()
        }

        tracks?.let { tracks ->
            tracks.forEach {
                val coordinates = it.coordinates
                val polyline = googleMap?.addPolyline(
                    DirectionConverter.createPolyline(
                        this@MainActivity,
                        NavigationManager.parseCoordinate(coordinates),
                        5,
                        ContextCompat.getColor(this, R.color.colorAccent)
                    )
                )
                if (polyline != null) {
                    trackPolylines.add(polyline)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        navigationStarted = NavigationManager.isNavigationStarted()

        if ((navigationStarted)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            stopService(Intent(this, GPSService::class.java))
            if (EventBus.getDefault().isRegistered(this)) {
                EventBus.getDefault().unregister(this)
            }
            EventBus.getDefault().register(this)

            if (googleMap != null && NavigationManager.getReports() != null) {
                reports = NavigationManager.getReports()!!
                clearReports()
                populateReportMarkers()
            }
        } else {
            currentLocationMarker?.setIcon(Utilities.BitmapHelper.resToBitmap(R.drawable.ic_user_location))
            binding.btnStart.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.green
                )
            )
            viewModel.primaryButtonText.value = getString(R.string.label_navigation_start)
            viewModel.routeNameVisibility.value = View.GONE
            viewModel.navigationBtnVisibility.value = View.GONE
            viewModel.nonNavigationBtnVisibility.value = View.VISIBLE
            EventBus.getDefault().unregister(this)
            clearReports()
            NavigationManager.clearNavigation()
        }
        startLocationUpdate()
    }

    override fun onPause() {
        super.onPause()
        if ((navigationStarted)) {
            startForegroundService(Intent(this, GPSService::class.java))
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            EventBus.getDefault().unregister(this)
        }
        stopLocationUpdate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_ACCESS -> {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        REQUEST_LOCATION_ACCESS
                    )
                } else {
                    startLocationUpdate()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_NEW_REPORT -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (NavigationManager.getReports() != null) {
                        clearReports()
                        reports = NavigationManager.getReports()!!
                        populateReportMarkers()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
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
            //LogManager.log("New location received")
            viewModel.gpsStatusVisibility.value = View.GONE

            val currentLocation = NavigationManager.navigationLocationUpdate(this@MainActivity, location, textToSpeech)

            if (currentLocation != null && googleMap != null) {
                this@MainActivity.currentLocation = currentLocation
                if (navigationStarted) {

                    if (isFollowLocation) {
                        viewModel.recenterVisibility.value = View.GONE

                        val cameraBearing: Float? = if (!location.hasBearing()) {
                            NavigationManager.getLastBearing()
                        } else {
                            location.bearing
                        }
                        animateCurrentLocation(currentLocation.newLocation, null, cameraBearing)
                    }

                    currentLocationMarker?.remove()

                    if (location.hasBearing()) {
                        NavigationManager.setNewBearing(location.bearing)
                        currentLocationMarker = googleMap!!.addMarker(
                            MarkerOptions().position(
                                currentLocation.newLocation
                            )
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
                                .flat(true)
                                .rotation(location.bearing)
                        )
                    } else {
                        val lastBearing = NavigationManager.getLastBearing()
                        if (lastBearing != null) {
                            currentLocationMarker = googleMap!!.addMarker(
                                MarkerOptions().position(
                                    currentLocation.newLocation
                                )
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
                                    .flat(true)
                                    .rotation(lastBearing)
                            )
                        } else {
                            currentLocationMarker = googleMap!!.addMarker(
                                MarkerOptions().position(
                                    currentLocation.newLocation
                                )
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
                                    .flat(true)
                            )
                        }
                    }

                    currentLocationMarker?.setAnchor(0.5f, 0.5f)
                    viewModel.speed.value = currentLocation.speed.toString()
                } else {
                    if (isFollowLocation) {
                        viewModel.recenterVisibility.value = View.GONE
                        moveCurrentLocation(currentLocation.newLocation)
                    }

                    if (location.hasBearing()) {
                        NavigationManager.setNewBearing(location.bearing)
                    }

                    if (currentLocationMarker != null) {
                        currentLocationMarker!!.remove()
                    }
                    currentLocationMarker = googleMap!!.addMarker(
                        MarkerOptions().position(
                            LatLng(location.latitude, location.longitude)
                        )
                            .icon(Utilities.BitmapHelper.resToBitmap(R.drawable.ic_user_location))
                            .flat(true)
                        //.rotation(location.bearing)
                    )
                    currentLocationMarker?.setAnchor(0.5f, 0.5f)
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }
    }

    private val onMapCameraMoveStartedListener = GoogleMap.OnCameraMoveStartedListener {
        if (it == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            viewModel.recenterVisibility.value = View.VISIBLE
            isFollowLocation = false
        }
    }


    private fun populateReports() {
        clearReports()
        getReports()
    }

    private fun getReports() {
        viewModel.getReports().observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING ->
                        viewModel.showProgress()
                    Status.ERROR -> {
                        viewModel.hideProgress()
                        DialogManager.showErrorDialog(
                            this,
                            getString(R.string.error_default_message)
                        )
                    }
                    Status.SUCCESS -> {
                        viewModel.hideProgress()
                        it.data?.let { reports ->
                            this.reports = reports
                            NavigationManager.setReports(reports)
                            populateReportMarkers()
                        }
                    }
                }
            }
        })
    }

    private fun populateReportMarkers() {
        doAsync {
            reports.forEachIndexed { _, report ->
                val track = ConfigManager.getTrack(report.trackKey)

                if (track != null) {
                    val reportPosition = NavigationManager.findNearestPoint(
                        LatLng(report.latitude, report.longitude),
                        NavigationManager.parseCoordinate(track.coordinates)
                    )
                    addReportToMap(report, reportPosition)
                } else {
                    addReportToMap(report)
                }
            }

            reports.sortBy {
                it.mapIndex
            }
        }
    }

    private fun addReportToMap(report: Report, reportPosition: LatLng? = null) {
        doAsync {
            val markerBitmap = getBitmapFromLink(report.reportType.imageUrl)
            activityUiThread {
                if (markerBitmap != null) {
                    if (reportPosition != null) {
                        reportMarkers.add(
                            googleMap!!.addMarker(
                                MarkerOptions().position(
                                    LatLng(reportPosition.latitude, reportPosition.longitude)
                                ).icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            )
                        )
                    } else {
                        reportMarkers.add(
                            googleMap!!.addMarker(
                                MarkerOptions().position(
                                    LatLng(report.latitude, report.longitude)
                                ).icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                            )
                        )
                    }
                } else {
                    if (reportPosition != null) {
                        reportMarkers.add(
                            googleMap!!.addMarker(
                                MarkerOptions().position(
                                    LatLng(reportPosition.latitude, reportPosition.longitude)
                                )
                            )
                        )
                    } else {
                        reportMarkers.add(
                            googleMap!!.addMarker(
                                MarkerOptions().position(
                                    LatLng(report.latitude, report.longitude)
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun clearReports() {
        reportMarkers.forEach {
            it.remove()
        }
    }

    private fun moveCurrentLocation(position: LatLng?, zoom: Float? = null) {
        googleMap!!.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                position, zoom ?: Constants.Config.LOCATION_MAP_ZOOM
            )
        )
    }

    private fun animateCurrentLocation(
        position: LatLng?,
        zoom: Float? = null,
        bearing: Float? = null
    ) {
        if (!mapCameraAnimationInProgress && googleMap != null && position != null) {
            mapCameraAnimationInProgress = true

            if (bearing != null) {
                val cameraPosition = CameraPosition.Builder()
                    .target(position)
                    .zoom(zoom ?: Constants.Config.LOCATION_MAP_ZOOM)
                    //.bearing(bearing)
                    .build()

                googleMap!!.animateCamera(
                    CameraUpdateFactory.newCameraPosition(cameraPosition),
                    cancelableCallback
                )
            } else {
                googleMap!!.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        position, zoom ?: Constants.Config.LOCATION_MAP_ZOOM
                    ), cancelableCallback
                )
            }
        }
    }

    private var cancelableCallback: CancelableCallback = object : CancelableCallback {
        override fun onFinish() {
            mapCameraAnimationInProgress = false
        }

        override fun onCancel() {
            mapCameraAnimationInProgress = false
        }
    }

    fun recenterAction(view: View) {
        isFollowLocation = true
        viewModel.recenterVisibility.value = View.GONE
        if (currentLocationMarker != null) {
            animateCurrentLocation(currentLocationMarker!!.position)
        }
    }

    fun reportAction(view: View) {
        if (currentLocation != null) {
            startRevealActivity(
                binding.root,
                NewReportActivity.newIntent(this, currentLocation!!),
                REQUEST_NEW_REPORT
            )
        } else {
            DialogManager.showErrorDialog(
                this,
                getString(R.string.error_gps_not_available_title),
                getString(R.string.error_gps_not_available_message)
            )
        }
    }

    fun settingsAction(view: View) {
        startActivity(SettingsActivity.newIntent(this))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewReportReceived(reportEvent: ReportEvent) {
        //LogManager.log("New Report Received")
        if (NavigationManager.getReports() != null) {
            reports = NavigationManager.getReports()!!

            when (reportEvent.type) {
                NotificationType.REPORT -> {
                    val track = ConfigManager.getTrack(reportEvent.report.trackKey)
                    if (track != null) {
                        val reportPosition = NavigationManager.findNearestPoint(
                            LatLng(reportEvent.report.latitude, reportEvent.report.longitude),
                            NavigationManager.parseCoordinate(track.coordinates)
                        )
                        addReportToMap(reportEvent.report, reportPosition)
                    } else {
                        addReportToMap(reportEvent.report)
                    }
                }
                NotificationType.REPORT_DELETED -> {
                    clearReports()
                    populateReportMarkers()
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateUIReceived(updateUIEvent: UpdateUIEvent) {
        //LogManager.log("Update UI Received: $updateUIEvent")

        if (updateUIEvent.locationUpdate.trackDirection != null) {
            viewModel.routeNameVisibility.value = View.VISIBLE
            if (updateUIEvent.locationUpdate.trackDirection == LocationUpdate.Direction.FORWARD) {

                viewModel.routeName.value =
                    "${updateUIEvent.locationUpdate.trackFrom} -> ${updateUIEvent.locationUpdate.trackTo}"
            } else {
                viewModel.routeName.value =
                    "${updateUIEvent.locationUpdate.trackTo} -> ${updateUIEvent.locationUpdate.trackFrom}"
            }
        } else {
            if (updateUIEvent.locationUpdate.status != null) {
                viewModel.routeNameVisibility.value = View.VISIBLE
                viewModel.routeName.value = updateUIEvent.locationUpdate.status.toString()
            } else {
                viewModel.routeNameVisibility.value = View.GONE
                viewModel.routeName.value = ""
            }
        }

        if (updateUIEvent.upcomingReport != null) {
            viewModel.reportVisibility.value = View.VISIBLE
            viewModel.reportID.value = "Report #${updateUIEvent.upcomingReport.id}"
            viewModel.reportImage.value = updateUIEvent.upcomingReport.reportType.imageUrl
            viewModel.reportName.value = updateUIEvent.upcomingReport.reportType.name
            viewModel.reportDistance.value =
                Utilities.DistanceHelper.formatMeter(updateUIEvent.upcomingReportDistance)

            if (updateUIEvent.upcomingReportDistance != null) {
                if (updateUIEvent.upcomingReportDistance < 1000.toDouble()) {
                    setReportVisibilityButtons(updateUIEvent.upcomingReport.id)
                    viewModel.reportConfirmationVisibility.value = View.VISIBLE

                } else {
                    viewModel.reportConfirmationVisibility.value = View.GONE
                    resetReportVisibilityButtons()
                }
            } else {
                viewModel.reportConfirmationVisibility.value = View.GONE
                resetReportVisibilityButtons()
            }
        } else {
            viewModel.reportVisibility.value = View.GONE
            viewModel.reportConfirmationVisibility.value = View.GONE
            resetReportVisibilityButtons()
            viewModel.reportID.value = ""
            viewModel.reportImage.value = ""
            viewModel.reportName.value = ""
            viewModel.reportDistance.value = ""
        }
    }

    private fun setReportVisibilityButtons(reportId: Int) {
        binding.btnReportInvisible.setOnClickListener {
            setReportVisibility(reportId, false)
        }

        binding.btnReportVisible.setOnClickListener {
            setReportVisibility(reportId, true)
        }
    }

    private fun resetReportVisibilityButtons() {
        binding.btnReportInvisible.setOnClickListener(null)
        binding.btnReportVisible.setOnClickListener(null)
    }

    private fun setReportVisibility(reportId: Int, isVisible: Boolean) {
        viewModel.setReportVisibility(reportId, isVisible).observe(this, Observer {
            it?.let { resource ->
                when (resource.status) {
                    Status.LOADING ->
                        viewModel.showProgress()
                    Status.ERROR -> {
                        viewModel.hideProgress()
                        DialogManager.showErrorDialog(
                            this,
                            getString(R.string.error_default_message)
                        )
                    }
                    Status.SUCCESS -> {
                        viewModel.hideProgress()
                    }
                }
            }
        })
    }
}