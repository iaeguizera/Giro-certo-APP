package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.ui.MainScreen
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MyApplication
        val factory = MainViewModelFactory(
            geocodeAddressUseCase = app.geocodeAddressUseCase,
            calculateMatrixUseCase = app.calculateMatrixUseCase,
            optimizeRouteUseCase = app.optimizeRouteUseCase,
            getCurrentLocationUseCase = app.getCurrentLocationUseCase,
            savedStopDao = app.database.savedStopDao(),
            preferencesRepository = app.preferencesRepository
        )

        setContent {
            val viewModel: MainViewModel = viewModel(factory = factory)

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
