package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Coordinate

object WazeIntentHelper {

    /**
     * Opens Waze to navigate to a specific latitude and longitude.
     * Redirects to Play Store if Waze is not installed.
     */
    fun openWaze(context: Context, lat: Double, lng: Double) {
        val uri = "waze://?ll=$lat,$lng&navigate=yes"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Waze não instalado. Redirecionando...", Toast.LENGTH_SHORT).show()
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.waze"))
            playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(playStoreIntent)
            } catch (ex: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.waze"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(webIntent)
            }
        }
    }

    /**
     * Opens Google Maps to navigate to a specific latitude and longitude.
     * Redirects to web maps if not installed.
     */
    fun openGoogleMaps(context: Context, lat: Double, lng: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(webIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Google Maps não pode ser aberto.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Opens the full multi-stop optimized route.
     * Google Maps is used as it natively supports multi-waypoint deep links, while
     * Waze only supports single-destination routing deep links.
     * Selects up to 10 waypoints (origin, destination, and up to 8 evenly-spaced intermediates).
     */
    fun openCompleteRoute(context: Context, coordinates: List<Coordinate>) {
        if (coordinates.size < 2) return

        val selectedPoints = mutableListOf<Coordinate>()
        selectedPoints.add(coordinates.first()) // Origin

        if (coordinates.size <= 10) {
            selectedPoints.addAll(coordinates.subList(1, coordinates.size))
        } else {
            // Select 8 intermediate coordinates with uniform spacing
            val totalIntermediates = coordinates.size - 2
            val step = totalIntermediates.toDouble() / 8.0
            for (i in 0 until 8) {
                val index = 1 + (i * step).toInt()
                if (index < coordinates.size - 1) {
                    val pt = coordinates[index]
                    if (!selectedPoints.contains(pt)) {
                        selectedPoints.add(pt)
                    }
                }
            }
            selectedPoints.add(coordinates.last()) // Destination
        }

        val origin = selectedPoints.first()
        val destination = selectedPoints.last()
        val waypoints = if (selectedPoints.size > 2) {
            selectedPoints.subList(1, selectedPoints.size - 1).joinToString("|") { "${it.lat},${it.lng}" }
        } else {
            ""
        }

        val mapsUri = Uri.Builder()
            .scheme("https")
            .authority("www.google.com")
            .path("maps/dir/")
            .appendQueryParameter("api", "1")
            .appendQueryParameter("origin", "${origin.lat},${origin.lng}")
            .appendQueryParameter("destination", "${destination.lat},${destination.lng}")
            .apply {
                if (waypoints.isNotEmpty()) {
                    appendQueryParameter("waypoints", waypoints)
                }
            }
            .build()

        val intent = Intent(Intent.ACTION_VIEW, mapsUri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir mapa completo.", Toast.LENGTH_SHORT).show()
        }
    }
}
