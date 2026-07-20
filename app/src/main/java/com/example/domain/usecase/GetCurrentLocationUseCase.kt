package com.example.domain.usecase

import android.content.Context
import com.example.data.model.Coordinate
import com.example.utils.LocationHelper

class GetCurrentLocationUseCase {
    suspend operator fun invoke(context: Context): Coordinate? {
        return LocationHelper.getCurrentLocation(context)
    }

    fun hasPermission(context: Context): Boolean {
        return LocationHelper.hasLocationPermission(context)
    }
}
