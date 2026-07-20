package com.example

import android.app.Application
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.remote.NetworkClient
import com.example.data.repository.GeocodingRepository
import com.example.data.repository.GeocodingRepositoryImpl
import com.example.data.repository.PreferencesRepository
import com.example.data.repository.RouteRepository
import com.example.data.repository.RouteRepositoryImpl
import com.example.domain.usecase.CalculateMatrixUseCase
import com.example.domain.usecase.GeocodeAddressUseCase
import com.example.domain.usecase.GetCurrentLocationUseCase
import com.example.domain.usecase.OptimizeRouteUseCase

class MyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var geocodingRepository: GeocodingRepository
        private set

    lateinit var routeRepository: RouteRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    // Use Cases
    lateinit var geocodeAddressUseCase: GeocodeAddressUseCase
        private set

    lateinit var calculateMatrixUseCase: CalculateMatrixUseCase
        private set

    lateinit var optimizeRouteUseCase: OptimizeRouteUseCase
        private set

    lateinit var getCurrentLocationUseCase: GetCurrentLocationUseCase
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "giro_certo_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        geocodingRepository = GeocodingRepositoryImpl(
            context = applicationContext,
            nominatimApi = NetworkClient.nominatimApi,
            cacheDao = database.geocodeCacheDao()
        )

        routeRepository = RouteRepositoryImpl(
            osrmApi = NetworkClient.osrmApi
        )

        preferencesRepository = PreferencesRepository(
            context = applicationContext
        )

        geocodeAddressUseCase = GeocodeAddressUseCase(geocodingRepository)
        calculateMatrixUseCase = CalculateMatrixUseCase(routeRepository)
        optimizeRouteUseCase = OptimizeRouteUseCase()
        getCurrentLocationUseCase = GetCurrentLocationUseCase()
    }
}
