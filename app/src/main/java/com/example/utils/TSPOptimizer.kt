package com.example.utils

object TSPOptimizer {

    fun nearestNeighbor(matrix: List<List<Double>>): List<Int> {
        val size = matrix.size
        if (size <= 1) return listOf(0)

        val tour = mutableListOf<Int>()
        val visited = BooleanArray(size)

        // Always start at index 0 (origin)
        tour.add(0)
        visited[0] = true

        var current = 0
        while (tour.size < size) {
            var next = -1
            var minDist = Double.MAX_VALUE
            for (i in 0 until size) {
                if (!visited[i]) {
                    val dist = matrix[current][i]
                    if (dist < minDist) {
                        minDist = dist
                        next = i
                    }
                }
            }
            if (next == -1) break
            tour.add(next)
            visited[next] = true
            current = next
        }

        return tour
    }

    fun bruteForce(matrix: List<List<Double>>): List<Int> {
        val size = matrix.size
        if (size <= 1) return listOf(0)
        if (size > 11) return nearestNeighbor(matrix) // Safety fallback for limit

        val arr = IntArray(size) { it }
        var bestCost = Double.MAX_VALUE
        var bestTour = arr.toList()

        fun calculateCost(): Double {
            var cost = 0.0
            for (i in 0 until size - 1) {
                cost += matrix[arr[i]][arr[i + 1]]
            }
            return cost
        }

        fun permute(start: Int) {
            if (start == size - 1) {
                val cost = calculateCost()
                if (cost < bestCost) {
                    bestCost = cost
                    bestTour = arr.toList()
                }
                return
            }

            for (i in start until size) {
                // Swap
                val temp = arr[start]
                arr[start] = arr[i]
                arr[i] = temp

                permute(start + 1)

                // Backtrack
                val tempBack = arr[start]
                arr[start] = arr[i]
                arr[i] = tempBack
            }
        }

        permute(1) // Keep index 0 (origin) fixed, permute starting from index 1
        return bestTour
    }
}
