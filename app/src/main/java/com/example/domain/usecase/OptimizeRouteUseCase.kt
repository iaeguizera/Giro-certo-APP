package com.example.domain.usecase

import com.example.utils.TSPOptimizer

class OptimizeRouteUseCase {
    operator fun invoke(distanceMatrix: List<List<Double>>, isExactMode: Boolean): List<Int> {
        val size = distanceMatrix.size
        return if (isExactMode && size <= 11) {
            TSPOptimizer.bruteForce(distanceMatrix)
        } else {
            TSPOptimizer.nearestNeighbor(distanceMatrix)
        }
    }
}
