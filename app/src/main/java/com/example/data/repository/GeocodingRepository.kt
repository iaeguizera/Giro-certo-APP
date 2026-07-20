package com.example.data.repository

import android.content.Context
import android.location.Geocoder
import com.example.data.local.GeocodeCacheDao
import com.example.data.local.GeocodeCacheEntity
import com.example.data.model.Coordinate
import com.example.data.remote.NominatimApi
import kotlinx.coroutines.delay
import java.util.Locale

interface GeocodingRepository {
    suspend fun geocode(address: String): Coordinate?
    suspend fun reverseGeocode(lat: Double, lng: Double): String?
    suspend fun getSuggestions(query: String): List<String>
}

class GeocodingRepositoryImpl(
    private val context: Context,
    private val nominatimApi: NominatimApi,
    private val cacheDao: GeocodeCacheDao
) : GeocodingRepository {

    override suspend fun geocode(address: String): Coordinate? {
        val trimmedAddress = address.trim()
        if (trimmedAddress.isEmpty()) return null

        // Check cache first
        val cached = cacheDao.getGeocode(trimmedAddress)
        if (cached != null) {
            return Coordinate(cached.lat, cached.lng)
        }

        // Support manual coordinate input e.g. "-23.5505, -46.6333"
        val coordPattern = """^(-?\d+\.?\d*)\s*,\s*(-?\d+\.?\d*)$""".toRegex()
        val match = coordPattern.find(trimmedAddress)
        if (match != null) {
            val (latStr, lngStr) = match.destructured
            val lat = latStr.toDoubleOrNull()
            val lng = lngStr.toDoubleOrNull()
            if (lat != null && lng != null) {
                val coord = Coordinate(lat, lng)
                // Save to cache
                cacheDao.insertGeocode(GeocodeCacheEntity(trimmedAddress, lat, lng, "Coordenada Manual"))
                return coord
            }
        }

        // Try Native Geocoder
        var coordinate: Coordinate? = null
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(trimmedAddress, 1)
                if (!results.isNullOrEmpty()) {
                    val addressObj = results[0]
                    coordinate = Coordinate(addressObj.latitude, addressObj.longitude)
                }
            } catch (e: Exception) {
                // Ignore and fallback to Nominatim
            }
        }

        if (coordinate != null) {
            cacheDao.insertGeocode(GeocodeCacheEntity(trimmedAddress, coordinate.lat, coordinate.lng, "Geocoder Nativo"))
            return coordinate
        }

        // Fallback to Nominatim API with exponential backoff and retry (max 3 times)
        var attempt = 1
        var delayMs = 1000L
        while (attempt <= 3) {
            try {
                val results = nominatimApi.search(query = trimmedAddress)
                if (results.isNotEmpty()) {
                    val res = results[0]
                    val lat = res.lat.toDoubleOrNull()
                    val lon = res.lon.toDoubleOrNull()
                    if (lat != null && lon != null) {
                        val resultCoord = Coordinate(lat, lon)
                        cacheDao.insertGeocode(
                            GeocodeCacheEntity(
                                addressQuery = trimmedAddress,
                                lat = lat,
                                lng = lon,
                                displayName = res.displayName
                            )
                        )
                        return resultCoord
                    }
                }
                break // No results found, don't retry
            } catch (e: Exception) {
                if (attempt == 3) {
                    break
                }
                delay(delayMs)
                delayMs *= 2
                attempt++
            }
        }

        return null
    }

    override suspend fun reverseGeocode(lat: Double, lng: Double): String? {
        // Try Native Geocoder
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(lat, lng, 1)
                if (!results.isNullOrEmpty()) {
                    val addrObj = results[0]
                    val addressLines = (0..addrObj.maxAddressLineIndex).map { addrObj.getAddressLine(it) }
                    return addressLines.joinToString(", ")
                }
            } catch (e: Exception) {
                // Ignore and fallback
            }
        }

        // Fallback to Nominatim Reverse API
        try {
            val response = nominatimApi.reverse(lat = lat, lon = lng)
            return response.displayName
        } catch (e: Exception) {
            // Ignore
        }

        return "$lat, $lng"
    }

    override suspend fun getSuggestions(query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.length < 3) return emptyList()

        // 1. Try Native Geocoder
        if (Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(trimmed, 5)
                if (!results.isNullOrEmpty()) {
                    return results.mapNotNull { addr ->
                        val lines = (0..addr.maxAddressLineIndex).map { addr.getAddressLine(it) }
                        if (lines.isNotEmpty()) lines.joinToString(", ") else addr.featureName
                    }
                }
            } catch (e: Exception) {
                // Fail silently and fallback
            }
        }

        // 2. Fallback to Nominatim search with limit 5
        try {
            val results = nominatimApi.search(query = trimmed, limit = 5)
            return results.mapNotNull { it.displayName }
        } catch (e: Exception) {
            // Fail silently
        }

        return emptyList()
    }
}
