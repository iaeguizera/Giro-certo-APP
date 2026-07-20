package com.example.domain.usecase

import com.example.data.model.Coordinate
import com.example.data.repository.GeocodingRepository

class GeocodeAddressUseCase(private val repository: GeocodingRepository) {
    suspend operator fun invoke(address: String): Coordinate? {
        return repository.geocode(address)
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): String? {
        return repository.reverseGeocode(lat, lng)
    }

    suspend fun getSuggestions(query: String): List<String> {
        return repository.getSuggestions(query)
    }
}
