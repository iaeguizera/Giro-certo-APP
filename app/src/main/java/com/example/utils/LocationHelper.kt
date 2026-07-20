package com.example.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.data.model.Coordinate
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object LocationHelper {

    /**
     * Checks if location permission is granted.
     */
    fun hasLocationPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Retrieves current latitude and longitude.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Coordinate? {
        if (!hasLocationPermission(context)) return null

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val lastLocation: Location? = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                Coordinate(lastLocation.latitude, lastLocation.longitude)
            } else {
                val cts = CancellationTokenSource()
                val freshLocation = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).await()
                if (freshLocation != null) {
                    Coordinate(freshLocation.latitude, freshLocation.longitude)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
