package com.example.utils

import java.io.BufferedReader
import java.io.StringReader

object CsvParser {
    /**
     * Parses CSV text. Supports comma or semicolon delimiters.
     * Detects and ignores header row if it contains typical labels.
     * Extract addresses or coordinates.
     */
    fun parseCsv(csvText: String): List<String> {
        val addresses = mutableListOf<String>()
        val reader = BufferedReader(StringReader(csvText))
        var line: String? = reader.readLine()
        var isFirst = true

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val delimiter = if (trimmed.contains(";")) ";" else ","
                val parts = trimmed.split(delimiter).map { it.trim().removeSurrounding("\"") }

                if (parts.isNotEmpty()) {
                    val firstLower = parts[0].lowercase()
                    val isHeader = isFirst && (
                        firstLower == "endereco" || firstLower == "endereço" || firstLower == "address" || 
                        firstLower == "latitude" || firstLower == "lat" || firstLower == "origem" || firstLower == "local"
                    )
                    
                    if (!isHeader) {
                        if (parts.size >= 3) {
                            val addr = parts[0]
                            val lat = parts[1].replace(",", ".").toDoubleOrNull()
                            val lng = parts[2].replace(",", ".").toDoubleOrNull()
                            if (lat != null && lng != null) {
                                addresses.add("$lat, $lng")
                            } else {
                                if (addr.isNotEmpty()) addresses.add(addr)
                            }
                        } else {
                            val firstPart = parts[0]
                            if (firstPart.isNotEmpty()) {
                                addresses.add(firstPart)
                            }
                        }
                    }
                }
            }
            isFirst = false
            line = reader.readLine()
        }
        return addresses
    }
}
