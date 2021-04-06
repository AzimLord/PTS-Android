package com.ktmb.pts.data.model

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LocationUpdate(
    val speed: Int,
    val newLocation: LatLng,
    val distanceFromOriginalLocation: Float,
    val trackKey: String,
    val trackFrom: String,
    val trackTo: String,
    var status: Status? = null,
    var trackDirection: Direction? = null,
    var trackBearing: Float? = null
): Parcelable {

    enum class Status {
        MOVING,
        STOPPED
    }

    enum class Direction {
        FORWARD,
        REVERSE
    }

}