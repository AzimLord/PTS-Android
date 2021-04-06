package com.ktmb.pts.ui.report.view

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.akexorcist.googledirection.util.DirectionConverter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.ktmb.pts.R
import com.ktmb.pts.base.BaseActivity
import com.ktmb.pts.data.model.LocationUpdate
import com.ktmb.pts.data.model.Track
import com.ktmb.pts.databinding.ActivityNewReportPathBinding
import com.ktmb.pts.ui.report.viewmodel.NewReportPathViewModel
import com.ktmb.pts.utilities.*
import org.jetbrains.anko.collections.forEachByIndex

class NewReportPathActivity : BaseActivity() {

    private lateinit var binding: ActivityNewReportPathBinding
    private lateinit var viewModel: NewReportPathViewModel
    private lateinit var googleMap: GoogleMap
    private lateinit var currentLocationMarker: Marker
    private lateinit var currentLocation: LocationUpdate
    private var tracks: ArrayList<Track>? = null
    private var trackPolylines = ArrayList<Polyline>()
    private var distanceSelected = 300.0
    private var reportPolyline: Polyline? = null

    companion object {
        private const val EXTRA_CURRENT_LOCATION = "currentLocation"

        fun newIntent(context: Context, currentLocation: LocationUpdate): Intent {
            val intent = Intent(context, NewReportPathActivity::class.java)
            intent.putExtra(EXTRA_CURRENT_LOCATION, currentLocation)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_new_report_path
        )

        viewModel = ViewModelProvider.NewInstanceFactory().create(NewReportPathViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        currentLocation = intent.extras!!.getParcelable(EXTRA_CURRENT_LOCATION)!!

        (supportFragmentManager.findFragmentById(R.id.maps) as SupportMapFragment).getMapAsync { googleMap ->
            this.googleMap = googleMap
            this.googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this, R.raw.google_map_light
                )
            )
            showCurrentLocation(currentLocation.newLocation)
            displayTracks()
        }
    }

    override fun setToolbar() {
        super.setToolbar()
        supportActionBar?.title = "Send Path Report"
    }

    private fun showCurrentLocation(position: LatLng?, zoom: Float? = null) {
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                position, zoom ?: Constants.Config.LOCATION_MAP_ZOOM
            )
        )

        currentLocationMarker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(currentLocation.newLocation.latitude, currentLocation.newLocation.longitude))
                .flat(true)
                .draggable(true)
        )

        googleMap.setOnMarkerDragListener(object: GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(p0: Marker?) {
                // Do nothing
            }

            override fun onMarkerDrag(p0: Marker?) {
                // Do nothing
            }

            override fun onMarkerDragEnd(marker: Marker) {
                reportPolyline?.remove()
                drawReportPath(marker)
            }

        })
    }

    private fun displayTracks() {
        tracks = ConfigManager.getTracks()

        trackPolylines.forEach {
            it.remove()
        }

        tracks?.let { tracks ->
            tracks.forEach {
                val coordinates = it.coordinates
                val polyline = googleMap.addPolyline(
                    DirectionConverter.createPolyline(
                        this@NewReportPathActivity,
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

    private fun drawReportPath(marker: Marker) {

        val distanceInMeter = distanceSelected
        val location = getLocation(marker.position)
        val coordinates = ConfigManager.getTrack(location?.trackKey)
        var pos = 0

        if (location != null && coordinates != null) {
            val trackIndex = PolyUtil.locationIndexOnPath(
                location.newLocation,
                NavigationManager.parseCoordinate(coordinates.coordinates),
                true
            )

            val pointB = coordinates.coordinates[trackIndex + 1]

            val polylineCoordinates = ArrayList<LatLng>()
            polylineCoordinates.add(location.newLocation)
            polylineCoordinates.add(LatLng(pointB.latitude, pointB.longitude))

            var totalDistance = 0.0

            for (i in trackIndex until NavigationManager.parseCoordinate(coordinates.coordinates).size) {
                if (pos == 0) {
                    totalDistance += SphericalUtil.computeDistanceBetween(location.newLocation, NavigationManager.parseCoordinate(coordinates.coordinates)[trackIndex + 1])
                } else {

                }

                pos++

                LogManager.log(" \nNumber of loop: $pos\nTotal distance: $totalDistance", "NEW_REPORT")

                if (totalDistance >= distanceInMeter) {
                    break
                }
            }

            reportPolyline = googleMap.addPolyline(
                DirectionConverter.createPolyline(
                    this,
                    polylineCoordinates,
                    5,
                    ContextCompat.getColor(this, R.color.red)
                )
            )
        }

    }

    private fun getLocation(latLng: LatLng): LocationUpdate? {
        val originalLocation = Location("")
        originalLocation.latitude = latLng.latitude
        originalLocation.longitude = latLng.longitude

        if (tracks != null) {
            val newPossibleLocation = ArrayList<Location>()
            var newLocationUpdate: LocationUpdate? = null

            tracks!!.forEach {
                val nearestPoint = NavigationManager.findNearestPoint(
                    LatLng(originalLocation.latitude, originalLocation.longitude),
                    NavigationManager.parseCoordinate(it.coordinates)
                )
                if (nearestPoint != null) {
                    val location = Location("")
                    location.latitude = nearestPoint.latitude
                    location.longitude = nearestPoint.longitude
                    newPossibleLocation.add(location)
                }
            }

            newPossibleLocation.forEach {
                tracks!!.forEach { track ->
                    val distanceFromOriginalLocation = originalLocation.distanceTo(it)

                    if (newLocationUpdate == null) {
                        newLocationUpdate = LocationUpdate(
                            0,
                            LatLng(it.latitude, it.longitude),
                            distanceFromOriginalLocation,
                            track.key,
                            track.from,
                            track.to
                        )
                    } else {
                        if (PolyUtil.isLocationOnPath(
                                LatLng(it.latitude, it.longitude),
                                NavigationManager.parseCoordinate(track.coordinates),
                                false, 0.1
                            ) &&
                            newLocationUpdate!!.distanceFromOriginalLocation > distanceFromOriginalLocation
                        ) {
                            newLocationUpdate = LocationUpdate(
                                0,
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

            return newLocationUpdate
        }

        return null
    }

}