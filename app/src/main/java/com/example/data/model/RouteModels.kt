package com.example.data.model

data class Coordinate(val lat: Double, val lng: Double)

data class RouteStop(
    val id: Long = 0,
    val addressText: String,
    val coordinate: Coordinate?,
    val isOrigin: Boolean
)

data class RouteResult(
    val order: Int,
    val addressText: String,
    val coordinate: Coordinate,
    val segmentDistance: Double, // in km
    val segmentDuration: Double, // in minutes
    val accumulatedDistance: Double, // in km
    val accumulatedDuration: Double, // in minutes
    val isOrigin: Boolean
)
