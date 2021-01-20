package com.ktmb.pts.ui.main.view

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.StrictMode
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.akexorcist.googledirection.util.DirectionConverter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.data.model.Report
import com.ktmb.pts.data.model.Route
import com.ktmb.pts.databinding.ActivityMainBinding
import com.ktmb.pts.event.NewReportEvent
import com.ktmb.pts.service.GPSService
import com.ktmb.pts.ui.main.viewmodel.MainViewModel
import com.ktmb.pts.ui.report.view.NewReportActivity
import com.ktmb.pts.ui.route.view.RoutesActivity
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
    private var currentLocation: Marker? = null
    private var reports = ArrayList<Report>()
    private var reportMarkers = ArrayList<Marker>()
    private var currentRoute: Route? = null
    private var route: Polyline? = null
    private var isFollowLocation = true
    private var navigationStarted = false
    private var locationUpdate = true
    private var mapCameraAnimationInProgress = false

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
            //this.googleMap?.isMyLocationEnabled = true

            binding.vRoute.run {
                googleMap?.setPadding(16.px, 24.px, 0, this.height + 24.px)
            }

            this.googleMap!!.setOnCameraMoveStartedListener(onMapCameraMoveStartedListener)

            init()
        }
    }


    @SuppressLint("MissingPermission")
    private fun init() {
        currentRoute = NavigationManager.getRoute()
        if (NavigationManager.getReports() != null) {
            reports = NavigationManager.getReports()!!
        }

        if (currentRoute != null) {
            setRoute(currentRoute!!, false)
            startNavigation()

            val coordinates = ArrayList(PolyUtil.decode(currentRoute?.polyline))
            populateReports(coordinates)
        }

        binding.vRouteContainer.post {
            binding.vRouteContainer.layoutTransition.addTransitionListener(object :
                LayoutTransition.TransitionListener {
                override fun startTransition(
                    transition: LayoutTransition?,
                    container: ViewGroup?,
                    view: View?,
                    transitionType: Int
                ) {
                    // Do nothing
                }

                override fun endTransition(
                    transition: LayoutTransition?,
                    container: ViewGroup?,
                    view: View?,
                    transitionType: Int
                ) {
                    binding.vRoute.post {
                        googleMap?.setPadding(16.px, 24.px, 0, binding.vRoute.height + 24.px)
                    }
                }
            })
        }

        val lastLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val lastLocationNetwork =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        var selectedLocation: LatLng? = null

        Log.e(
            "LAST LOCATION", "" +
                    "${lastLocationGPS?.latitude} ${lastLocationGPS?.longitude} " +
                    "${lastLocationNetwork?.latitude} ${lastLocationNetwork?.longitude} "
        )

        if (lastLocationGPS != null) {
            selectedLocation = LatLng(lastLocationGPS.latitude, lastLocationGPS.longitude)
            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    selectedLocation, Constants.Config.LOCATION_MAP_ZOOM
                )
            )
            animateCurrentLocation(selectedLocation)

            currentLocation = googleMap?.addMarker(
                MarkerOptions().position(selectedLocation)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user_location))
                    .flat(true)
            )
            //currentLocation?.setAnchor(0.5f, 0.5f)
            startLocationUpdate()
        } else if (lastLocationNetwork != null) {
            selectedLocation = LatLng(lastLocationNetwork.latitude, lastLocationNetwork.longitude)
            googleMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    selectedLocation, Constants.Config.LOCATION_MAP_ZOOM
                )
            )
            animateCurrentLocation(selectedLocation)

            currentLocation = googleMap?.addMarker(
                MarkerOptions().position(selectedLocation)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user_location))
                    .flat(true)
            )
            //currentLocation?.setAnchor(0.5f, 0.5f)
            startLocationUpdate()
        }

        binding.vRoute.setOnClickListener {
            startActivityForResult(RoutesActivity.newIntent(this), REQUEST_ROUTE)
        }

        binding.btnRemoveRoute.setOnClickListener {
            removeRoute()
        }

        binding.btnStart.setOnClickListener {
            startNavigation()
        }

        binding.btnStop.setOnClickListener {
            stopNavigation()
        }

        binding.vRoute.post {
            googleMap?.setPadding(16.px, 24.px, 0, binding.vRoute.height + 24.px)
        }

        binding.btnReport.setOnClickListener {
            if (currentLocation != null) {
                val latitude = currentLocation!!.position.latitude
                val longitude = currentLocation!!.position.longitude
                startRevealActivity(binding.root, NewReportActivity.newIntent(this, latitude, longitude), REQUEST_NEW_REPORT)
            } else {
                DialogManager.showErrorDialog(
                    this,
                    getString(R.string.error_gps_not_available_title),
                    getString(R.string.error_gps_not_available_message)
                )
            }
        }

        binding.btnRecenter.setOnClickListener {
            isFollowLocation = true
            binding.btnRecenter.visibility = View.GONE
            if (currentLocation != null) {
                animateCurrentLocation(currentLocation!!.position)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if ((navigationStarted && currentRoute != null) || locationUpdate) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            stopService(Intent(this, GPSService::class.java))
            EventBus.getDefault().register(this)

            if (googleMap != null && NavigationManager.getReports() != null) {
                reports = NavigationManager.getReports()!!
                clearReports()
                populateReportMarkers()
            }
        }
        startLocationUpdate()
    }

    override fun onPause() {
        super.onPause()
        if ((navigationStarted && currentRoute != null)) {
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
            REQUEST_ROUTE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.extras?.let {
                        val route = it.getParcelable<Route>(RoutesActivity.EXTRA_ROUTE) as Route
                        setRoute(route)
                    }
                }
            }
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

    private fun setRoute(route: Route, showOverview: Boolean = true) {
        isFollowLocation = false
        currentRoute = route
        binding.tvRouteCode.text = route.code

        binding.vDirection.visibility = View.VISIBLE
        binding.tvFromStation.text = route.fromStation.name
        binding.tvToStation.text = route.toStation.name
        binding.btnRemoveRoute.visibility = View.VISIBLE
        binding.btnRecenter.visibility = View.GONE
        binding.btnStart.visibility = View.VISIBLE
        binding.btnStop.visibility = View.GONE
        binding.vSpeed.visibility = View.GONE
        binding.btnReport.visibility = View.GONE

        this.route?.remove()
        val coordinates = ArrayList(PolyUtil.decode(route.polyline))
        this.route = googleMap?.addPolyline(
            DirectionConverter.createPolyline(
                this@MainActivity,
                coordinates,
                5,
                ContextCompat.getColor(this, R.color.colorAccent)
            )
        )

        binding.vRoute.post {
            googleMap?.setPadding(16.px, 24.px, 0, binding.vRoute.height + 24.px)

            if (showOverview) {
                val routeBound = LatLngBounds.builder()
                    .include(LatLng(coordinates.first().latitude, coordinates.first().longitude))
                    .include(LatLng(coordinates.last().latitude, coordinates.last().longitude))
                    .build()
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(routeBound, 24.px))
            }
        }

        populateReports(coordinates)
        stopLocationUpdate()
    }

    private fun removeRoute() {
        isFollowLocation = true
        binding.tvRouteCode.text = ""

        binding.vDirection.visibility = View.GONE
        binding.tvFromStation.text = ""
        binding.tvToStation.text = ""
        binding.btnRemoveRoute.visibility = View.GONE

        binding.vRoute.post {
            googleMap?.setPadding(16.px, 24.px, 0, binding.vRoute.height + 24.px)
        }

        this.route?.remove()
        clearReports()
        startLocationUpdate()

        if (currentLocation != null) {
            animateCurrentLocation(currentLocation!!.position)
        }
    }

    private fun startNavigation() {
        if (currentRoute != null) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            NavigationManager.setRoute(currentRoute!!)
            NavigationManager.setReports(reports)
            isFollowLocation = true
            navigationStarted = true
            stopLocationUpdate()
            startLocationUpdate()
            if (currentLocation != null) {
                animateCurrentLocation(currentLocation!!.position)
            }

            binding.btnRemoveRoute.visibility = View.GONE
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
            binding.vSpeed.visibility = View.VISIBLE
            binding.btnReport.visibility = View.VISIBLE
        } else {
            DialogManager.showAlertDialog(
                this,
                getString(R.string.error_default_title),
                "Route not available"
            )
        }
    }

    private fun stopNavigation() {
        isFollowLocation = false
        navigationStarted = false
        stopLocationUpdate()

        if (currentRoute != null) {
            NavigationManager.clearNavigation()
            setRoute(currentRoute!!)
            startLocationUpdate()
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
        locationManager.registerGnssStatusCallback(gnssStatusCallback)
        locationUpdate = true
        viewModel.gpsIsAvailable.observe(this, Observer<Boolean> {
            binding.vGpsStatus.visibility = if (it) View.GONE else View.VISIBLE
        })
    }

    private fun stopLocationUpdate() {
        locationManager.removeUpdates(locationCallback)
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        locationUpdate = false
        viewModel.gpsIsAvailable.removeObservers(this)
    }

    private val locationCallback = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            LogManager.log("New location received")
            viewModel.gpsIsAvailable.value = true
            if (route != null && navigationStarted) {
                val mapUpdate = NavigationManager.navigationLocationUpdate(
                    currentLocation?.position,
                    location,
                    ArrayList(route!!.points),
                    reports,
                    textToSpeech
                )
                if (mapUpdate != null && googleMap != null) {
                    if (isFollowLocation) {
                        animateCurrentLocation(mapUpdate.newLocation)
                    }

                    currentLocation?.remove()
                    currentLocation = googleMap!!.addMarker(
                        MarkerOptions().position(
                            mapUpdate.newLocation
                        )
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location))
                            .flat(true)
                            .rotation(location.bearing)
                    )
                    currentLocation?.setAnchor(0.5f, 0.5f)
                    binding.tvSpeed.text = mapUpdate.speed.toString()
                }
            } else {
                with(googleMap!!) {
                    if (isFollowLocation) {
                        binding.btnRecenter.visibility = View.GONE
                        animateCurrentLocation(LatLng(location.latitude, location.longitude))
                    }

                    if (currentLocation != null) {
                        currentLocation!!.remove()
                    }
                    currentLocation = addMarker(
                        MarkerOptions().position(
                            LatLng(location.latitude, location.longitude)
                        )
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_user_location))
                            .flat(true)
                            .rotation(location.bearing)
                    )
                    currentLocation?.setAnchor(0.5f, 0.5f)
                }
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {

        }

        override fun onProviderEnabled(provider: String?) {

        }

        override fun onProviderDisabled(provider: String?) {

        }
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus?) {
            super.onSatelliteStatusChanged(status)
            LogManager.log("Satellite count: ${status?.satelliteCount}")
        }

        override fun onFirstFix(ttffMillis: Int) {
            super.onFirstFix(ttffMillis)
            LogManager.log("onFirstFix: $ttffMillis")
        }
    }

    private val onMapCameraMoveStartedListener = GoogleMap.OnCameraMoveStartedListener {
        if (it == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            binding.btnRecenter.visibility = View.VISIBLE
            isFollowLocation = false
        }
    }


    private fun populateReports(routeCoordinates: ArrayList<LatLng>) {
        clearReports()
        if (NavigationManager.getReports() == null) {
            reports.clear()
            if (googleMap != null && route != null) {
                getReports(routeCoordinates)
                //reports = NavigationManager.getMockReports()
            }
        } else {
            populateReportMarkers()
        }
    }

    private fun getReports(routeCoordinates: ArrayList<LatLng>) {
        viewModel.getReports(routeCoordinates).observe(this, Observer {
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
                            populateReportMarkers()
                        }
                    }
                }
            }
        })
    }

    private fun populateReportMarkers() {
        reports.forEachIndexed { _, report ->
            val reportPosition = NavigationManager.findNearestPoint(
                LatLng(report.latitude, report.longitude),
                route!!.points
            )

            if (reportPosition != null) {
                doAsync {
                    val markerBitmap = getBitmapFromLink(report.reportType.imageUrl)
                    activityUiThread {
                        if (markerBitmap != null) {
                            reportMarkers.add(
                                googleMap!!.addMarker(
                                    MarkerOptions().position(
                                        reportPosition
                                    ).icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                                )
                            )
                        } else {
//                    reportMarkers.add(
//                        googleMap!!.addMarker(
//                            MarkerOptions().position(
//                                reportPosition
//                            )
//                        )
//                    )
                        }
                    }
                }
            }
        }

        reports.sortBy {
            it.mapIndex
        }
    }

    private fun clearReports() {
        reportMarkers.forEach {
            it.remove()
        }
    }

    private fun animateCurrentLocation(position: LatLng?, zoom: Float? = null) {
        if (!mapCameraAnimationInProgress && googleMap != null && position != null) {
            mapCameraAnimationInProgress = true

            googleMap!!.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    position, zoom ?: Constants.Config.LOCATION_MAP_ZOOM
                ), cancelableCallback
            )
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewReportReceived(newReportEvent: NewReportEvent) {
        LogManager.log("New Report Received")
        NavigationManager.newReport(newReportEvent.report)
        if (NavigationManager.getReports() != null) {
            reports = NavigationManager.getReports()!!
            clearReports()
            populateReportMarkers()
        }
    }
}