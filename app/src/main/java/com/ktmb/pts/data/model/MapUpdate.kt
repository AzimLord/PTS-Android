package com.ktmb.pts.data.model

import com.google.android.gms.maps.model.LatLng

data class MapUpdate(
    val speed: Int,
    val newLocation: LatLng
)