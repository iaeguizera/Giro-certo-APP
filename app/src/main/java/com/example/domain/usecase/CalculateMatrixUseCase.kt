package com.example.domain.usecase

import com.example.data.model.Coordinate
import com.example.data.repository.RouteRepository

class CalculateMatrixUseCase(private val repository: RouteRepository) {
    suspend operator fun invoke(coordinates: List<Coordinate>): Pair<List<List<Double>>, List<List<Double>>> {
        return repository.getDistanceMatrix(coordinates)
    }
}
