package com.example.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SavedStopDao
import com.example.data.local.SavedStopEntity
import com.example.data.model.Coordinate
import com.example.data.model.RouteResult
import com.example.data.repository.PreferencesRepository
import com.example.domain.usecase.CalculateMatrixUseCase
import com.example.domain.usecase.GeocodeAddressUseCase
import com.example.domain.usecase.GetCurrentLocationUseCase
import com.example.domain.usecase.OptimizeRouteUseCase
import com.example.utils.CsvParser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val route: List<RouteResult>) : UiState()
    data class Error(val message: String) : UiState()
}

data class StopDeliveryState(
    val status: String = "PENDENTE", // "PENDENTE", "ENTREGUE", "NAO_ENTREGUE"
    val packages: Int = 1,
    val complement: String = "",
    val bairro: String = ""
)

class MainViewModel(
    private val geocodeAddressUseCase: GeocodeAddressUseCase,
    private val calculateMatrixUseCase: CalculateMatrixUseCase,
    private val optimizeRouteUseCase: OptimizeRouteUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val savedStopDao: SavedStopDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _snackbarFlow = MutableSharedFlow<String>()
    val snackbarFlow: SharedFlow<String> = _snackbarFlow.asSharedFlow()

    val originInput = MutableStateFlow("")
    val destinationsInput = MutableStateFlow("")
    val destinationSearchInput = MutableStateFlow("")
    val isExactMode = MutableStateFlow(true)

    private val _deliveryStates = MutableStateFlow<Map<Int, StopDeliveryState>>(emptyMap())
    val deliveryStates: StateFlow<Map<Int, StopDeliveryState>> = _deliveryStates.asStateFlow()

    private val _activeStopOrder = MutableStateFlow<Int?>(null)
    val activeStopOrder: StateFlow<Int?> = _activeStopOrder.asStateFlow()

    private val _originSuggestions = MutableStateFlow<List<String>>(emptyList())
    val originSuggestions: StateFlow<List<String>> = _originSuggestions.asStateFlow()

    private val _destinationSuggestions = MutableStateFlow<List<String>>(emptyList())
    val destinationSuggestions: StateFlow<List<String>> = _destinationSuggestions.asStateFlow()

    init {
        viewModelScope.launch {
            originInput.collect { query ->
                // Only search if it does not match our currently selected suggestion (or has length >= 3)
                if (query.trim().length >= 3) {
                    try {
                        _originSuggestions.value = geocodeAddressUseCase.getSuggestions(query)
                    } catch (e: Exception) {
                        _originSuggestions.value = emptyList()
                    }
                } else {
                    _originSuggestions.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            destinationSearchInput.collect { query ->
                if (query.trim().length >= 3) {
                    try {
                        _destinationSuggestions.value = geocodeAddressUseCase.getSuggestions(query)
                    } catch (e: Exception) {
                        _destinationSuggestions.value = emptyList()
                    }
                } else {
                    _destinationSuggestions.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            preferencesRepository.themeModeFlow.collect { mode ->
                _themeMode.value = mode
            }
        }

        viewModelScope.launch {
            val stops = savedStopDao.getAllStops()
            val origin = stops.firstOrNull { it.isOrigin }
            val destinations = stops.filter { !it.isOrigin }

            if (origin != null) {
                originInput.value = origin.addressText
            }
            if (destinations.isNotEmpty()) {
                destinationsInput.value = destinations.joinToString("\n") { it.addressText }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun fetchCurrentLocationAsOrigin(context: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                if (!getCurrentLocationUseCase.hasPermission(context)) {
                    _snackbarFlow.emit("Permissão de localização negada.")
                    _uiState.value = UiState.Idle
                    return@launch
                }

                val coord = getCurrentLocationUseCase(context)
                if (coord != null) {
                    val addressText = geocodeAddressUseCase.reverseGeocode(coord.lat, coord.lng)
                    if (addressText != null) {
                        originInput.value = addressText
                        _snackbarFlow.emit("Origem definida para sua localização atual.")
                    } else {
                        originInput.value = "${coord.lat}, ${coord.lng}"
                        _snackbarFlow.emit("Origem definida por coordenadas.")
                    }
                } else {
                    _snackbarFlow.emit("Não foi possível obter a localização atual.")
                }
            } catch (e: Exception) {
                _snackbarFlow.emit("Erro ao obter localização: ${e.message}")
            } finally {
                _uiState.value = UiState.Idle
            }
        }
    }

    fun importCsv(csvText: String) {
        viewModelScope.launch {
            try {
                val parsed = CsvParser.parseCsv(csvText)
                if (parsed.isEmpty()) {
                    _snackbarFlow.emit("Nenhum endereço encontrado no CSV.")
                    return@launch
                }

                if (parsed.size > 50) {
                    _snackbarFlow.emit("O CSV excede o limite de 50 paradas (encontrados: ${parsed.size}).")
                    destinationsInput.value = parsed.take(50).joinToString("\n")
                } else {
                    destinationsInput.value = parsed.joinToString("\n")
                    _snackbarFlow.emit("${parsed.size} paradas importadas com sucesso!")
                }
            } catch (e: Exception) {
                _snackbarFlow.emit("Falha ao analisar arquivo CSV.")
            }
        }
    }

    fun selectOriginSuggestion(suggestion: String) {
        originInput.value = suggestion
        _originSuggestions.value = emptyList()
    }

    fun selectDestinationSuggestion(suggestion: String) {
        val currentText = destinationsInput.value.trim()
        val lines = if (currentText.isEmpty()) mutableListOf() else currentText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        if (!lines.contains(suggestion)) {
            lines.add(suggestion)
            destinationsInput.value = lines.joinToString("\n")
        }
        destinationSearchInput.value = ""
        _destinationSuggestions.value = emptyList()
    }

    fun clearOriginSuggestions() {
        _originSuggestions.value = emptyList()
    }

    fun clearDestinationSuggestions() {
        _destinationSuggestions.value = emptyList()
    }

    fun selectActiveStop(order: Int) {
        _activeStopOrder.value = order
    }

    fun updateStopStatus(order: Int, status: String) {
        val current = _deliveryStates.value.toMutableMap()
        val state = current[order] ?: StopDeliveryState()
        current[order] = state.copy(status = status)
        _deliveryStates.value = current
    }

    fun updateStopPackages(order: Int, packages: Int) {
        val current = _deliveryStates.value.toMutableMap()
        val state = current[order] ?: StopDeliveryState()
        current[order] = state.copy(packages = maxOf(1, packages))
        _deliveryStates.value = current
    }

    fun updateStopDetails(order: Int, complement: String, bairro: String) {
        val current = _deliveryStates.value.toMutableMap()
        val state = current[order] ?: StopDeliveryState()
        current[order] = state.copy(complement = complement, bairro = bairro)
        _deliveryStates.value = current
    }

    fun clearAllData() {
        viewModelScope.launch {
            originInput.value = ""
            destinationsInput.value = ""
            destinationSearchInput.value = ""
            _originSuggestions.value = emptyList()
            _destinationSuggestions.value = emptyList()
            _uiState.value = UiState.Idle
            savedStopDao.clearAllStops()
            _snackbarFlow.emit("Dados limpos com sucesso!")
        }
    }

    fun optimizeRoute() {
        val origin = originInput.value.trim()
        val destsText = destinationsInput.value.trim()

        if (origin.isEmpty()) {
            viewModelScope.launch { _snackbarFlow.emit("Insira o ponto de partida (Origem).") }
            return
        }

        if (destsText.isEmpty()) {
            viewModelScope.launch { _snackbarFlow.emit("Insira ao menos um endereço de destino.") }
            return
        }

        val rawDestinations = destsText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (rawDestinations.size > 50) {
            viewModelScope.launch { _snackbarFlow.emit("Limite estrito de 50 paradas excedido! (Atualmente: ${rawDestinations.size})") }
            return
        }

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val originCoordinate = geocodeAddressUseCase(origin)
                if (originCoordinate == null) {
                    _uiState.value = UiState.Error("Não foi possível geocodificar o ponto de partida (Origem).")
                    _snackbarFlow.emit("Falha ao localizar Origem.")
                    return@launch
                }

                val geocodedDestinations = mutableListOf<Pair<String, Coordinate>>()
                val failedDestinations = mutableListOf<String>()

                for (dest in rawDestinations) {
                    val coord = geocodeAddressUseCase(dest)
                    if (coord != null) {
                        geocodedDestinations.add(Pair(dest, coord))
                    } else {
                        failedDestinations.add(dest)
                    }
                }

                if (geocodedDestinations.isEmpty()) {
                    _uiState.value = UiState.Error("Nenhum endereço de destino pôde ser geocodificado.")
                    _snackbarFlow.emit("Nenhum destino localizado com sucesso.")
                    return@launch
                }

                savedStopDao.clearAllStops()
                savedStopDao.insertStop(
                    SavedStopEntity(
                        addressText = origin,
                        lat = originCoordinate.lat,
                        lng = originCoordinate.lng,
                        isOrigin = true,
                        orderIndex = 0
                    )
                )
                val destEntities = geocodedDestinations.mapIndexed { idx, item ->
                    SavedStopEntity(
                        addressText = item.first,
                        lat = item.second.lat,
                        lng = item.second.lng,
                        isOrigin = false,
                        orderIndex = idx + 1
                    )
                }
                savedStopDao.insertStops(destEntities)

                val allCoords = mutableListOf<Coordinate>()
                allCoords.add(originCoordinate)
                geocodedDestinations.forEach { allCoords.add(it.second) }

                val (distanceMatrix, durationMatrix) = calculateMatrixUseCase(allCoords)

                val tourIndices = optimizeRouteUseCase(distanceMatrix, isExactMode.value)

                val routeResults = mutableListOf<RouteResult>()
                var totalDistance = 0.0
                var totalDuration = 0.0

                for (i in tourIndices.indices) {
                    val nodeIndex = tourIndices[i]
                    val coord = allCoords[nodeIndex]
                    val isNodeOrigin = (nodeIndex == 0)
                    val addressText = if (isNodeOrigin) origin else geocodedDestinations[nodeIndex - 1].first

                    val (segDist, segDur) = if (i == 0) {
                        Pair(0.0, 0.0)
                    } else {
                        val prevNode = tourIndices[i - 1]
                        val distKm = distanceMatrix[prevNode][nodeIndex] / 1000.0
                        val durMin = durationMatrix[prevNode][nodeIndex] / 60.0
                        Pair(distKm, durMin)
                    }

                    totalDistance += segDist
                    totalDuration += segDur

                    routeResults.add(
                        RouteResult(
                            order = i + 1,
                            addressText = addressText,
                            coordinate = coord,
                            segmentDistance = segDist,
                            segmentDuration = segDur,
                            accumulatedDistance = totalDistance,
                            accumulatedDuration = totalDuration,
                            isOrigin = isNodeOrigin
                        )
                    )
                }

                val initialStates = routeResults.associate { stop ->
                    val parts = stop.addressText.split(",")
                    val bairroText = if (parts.size > 2) parts[2].trim() else if (parts.size > 1) parts[1].trim() else "Bairro Não Identificado"
                    val compText = if (parts.size > 1) parts[1].trim() else ""
                    stop.order to StopDeliveryState(
                        status = "PENDENTE",
                        packages = 1,
                        complement = compText,
                        bairro = bairroText
                    )
                }
                _deliveryStates.value = initialStates
                _activeStopOrder.value = routeResults.firstOrNull()?.order

                _uiState.value = UiState.Success(routeResults)

                if (failedDestinations.isNotEmpty()) {
                    _snackbarFlow.emit("Aviso: ${failedDestinations.size} endereços ignorados por falha ao localizar.")
                } else {
                    _snackbarFlow.emit("Rota otimizada com sucesso!")
                }

            } catch (e: Exception) {
                _uiState.value = UiState.Error("Ocorreu um erro ao processar a rota: ${e.message}")
                _snackbarFlow.emit("Falha na otimização.")
            }
        }
    }
}

class MainViewModelFactory(
    private val geocodeAddressUseCase: GeocodeAddressUseCase,
    private val calculateMatrixUseCase: CalculateMatrixUseCase,
    private val optimizeRouteUseCase: OptimizeRouteUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val savedStopDao: SavedStopDao,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(
                geocodeAddressUseCase,
                calculateMatrixUseCase,
                optimizeRouteUseCase,
                getCurrentLocationUseCase,
                savedStopDao,
                preferencesRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
