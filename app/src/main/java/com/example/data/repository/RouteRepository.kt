package com.example.data.repository

import com.example.data.model.Coordinate
import com.example.data.remote.OsrmApi

interface RouteRepository {
    suspend fun getDistanceMatrix(coordinates: List<Coordinate>): Pair<List<List<Double>>, List<List<Double>>>
}

class RouteRepositoryImpl(private val osrmApi: OsrmApi) : RouteRepository {

    override suspend fun getDistanceMatrix(coordinates: List<Coordinate>): Pair<List<List<Double>>, List<List<Double>>> {
        if (coordinates.isEmpty()) {
            return Pair(emptyList(), emptyList())
        }

        // Format coordinates: "lng,lat;lng,lat;lng,lat"
        // Note: OSRM uses longitude,latitude format.
        val coordString = coordinates.joinToString(";") { "${it.lng},${it.lat}" }

        return try {
            val response = osrmApi.getTable(coordinates = coordString)
            val distances = response.distances
            val durations = response.durations

            val size = coordinates.size
            val sanitizedDistances = List(size) { i ->
                List(size) { j ->
                    val value = distances?.getOrNull(i)?.getOrNull(j)
                    if (value == null || value.isInfinite() || value.isNaN()) 1000000.0 else value
                }
            }

            val sanitizedDurations = List(size) { i ->
                List(size) { j ->
                    val value = durations?.getOrNull(i)?.getOrNull(j)
                    if (value == null || value.isInfinite() || value.isNaN()) 1000000.0 else value
                }
            }

            Pair(sanitizedDistances, sanitizedDurations)
        } catch (e: Exception) {
            val size = coordinates.size
            val fallbackDistances = List(size) { List(size) { 1000000.0 } }
            val fallbackDurations = List(size) { List(size) { 1000000.0 } }
            Pair(fallbackDistances, fallbackDurations)
        }
    }
}
