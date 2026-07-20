package com.example.presentation.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.data.model.RouteResult
import kotlin.math.sin

@Composable
fun MapBackground(
    route: List<RouteResult>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    
    // Pulse animation for location markers
    val infiniteTransition = rememberInfiniteTransition(label = "map_animations")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Dynamic map elements (subtle random streets)
    val baseColor = Color(0xFF10121A) // Deep elegant dark navy/black background
    val secondaryColor = Color(0xFF171A26) // Slightly lighter dark background
    val waterColor = Color(0xFF14243B) // Dark navy water bodies
    val parkColor = Color(0xFF132A20) // Deep slate green parks
    val roadColor = Color(0xFF1D2436) // Low-alpha slate road lines
    val highwayColor = Color(0xFF28344D) // Main highway line color
    
    val routeLineColor = Color(0xFF00C8FF) // Glowing cyan for active route
    val originColor = Color(0xFFFF9800) // Brand Orange for starting point

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw Map Background
        drawRect(color = baseColor)

        // 2. Draw styled water bodies (subtle organic look)
        drawCircle(
            color = waterColor,
            radius = width * 0.35f,
            center = Offset(width * 0.1f, height * 0.8f)
        )
        drawRoundRect(
            color = waterColor,
            topLeft = Offset(width * 0.7f, height * 0.1f),
            size = Size(width * 0.4f, height * 0.2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(60f)
        )

        // 3. Draw styled parks (subtle soft green)
        drawCircle(
            color = parkColor,
            radius = width * 0.2f,
            center = Offset(width * 0.85f, height * 0.7f)
        )
        drawCircle(
            color = parkColor,
            radius = width * 0.15f,
            center = Offset(width * 0.2f, height * 0.2f)
        )

        // 4. Draw Procedural Street Grid (Night-mode lines crisscrossing)
        // Horizontal minor roads
        for (i in 0..12) {
            val y = height * (i / 12f)
            drawLine(
                color = roadColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 2f
            )
        }
        // Vertical minor roads
        for (i in 0..8) {
            val x = width * (i / 8f)
            drawLine(
                color = roadColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 2f
            )
        }

        // Major Highway (crisscross diagonal)
        val highwayPath1 = Path().apply {
            moveTo(0f, height * 0.3f)
            cubicTo(width * 0.3f, height * 0.25f, width * 0.6f, height * 0.55f, width, height * 0.5f)
        }
        drawPath(
            path = highwayPath1,
            color = highwayColor,
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )

        val highwayPath2 = Path().apply {
            moveTo(width * 0.2f, 0f)
            lineTo(width * 0.8f, height)
        }
        drawPath(
            path = highwayPath2,
            color = highwayColor,
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        // 5. Draw Active Route overlay if available
        if (route.isNotEmpty()) {
            val lats = route.map { it.coordinate.lat }
            val lngs = route.map { it.coordinate.lng }
            val minLat = lats.minOrNull() ?: -23.55
            val maxLat = lats.maxOrNull() ?: -23.54
            val minLng = lngs.minOrNull() ?: -46.64
            val maxLng = lngs.maxOrNull() ?: -46.63

            val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
            val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

            val padding = 0.20f // Ensure route does not touch screen edges
            val activeWidth = width * (1f - 2f * padding)
            val activeHeight = height * (1f - 2f * padding)

            // Calculate screen coordinates for route nodes
            val points = route.map { stop ->
                val px = width * padding + ((stop.coordinate.lng - minLng) / lngRange).toFloat() * activeWidth
                // Invert y axis for lat
                val py = height * padding + ((maxLat - stop.coordinate.lat) / latRange).toFloat() * activeHeight
                Offset(px, py)
            }

            // Draw polyline connecting stops
            if (points.size > 1) {
                val routePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                // Under glow path
                drawPath(
                    path = routePath,
                    color = routeLineColor.copy(alpha = 0.25f),
                    style = Stroke(width = 12f, cap = StrokeCap.Round)
                )
                // Sharp foreground path
                drawPath(
                    path = routePath,
                    color = routeLineColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }

            // Draw Pins / Node Indicators with text order values
            points.forEachIndexed { index, point ->
                val isOrigin = index == 0
                val nodeColor = if (isOrigin) originColor else routeLineColor
                
                // Pulsing outer halo for origin and active delivery
                if (isOrigin) {
                    drawCircle(
                        color = nodeColor.copy(alpha = pulseAlpha),
                        radius = 28f * pulseScale,
                        center = point
                    )
                }

                // Inner core
                drawCircle(
                    color = nodeColor,
                    radius = 12f,
                    center = point
                )
                drawCircle(
                    color = baseColor,
                    radius = 8f,
                    center = point
                )
                drawCircle(
                    color = nodeColor,
                    radius = 5f,
                    center = point
                )

                // Label tag above node
                val label = if (isOrigin) "Partida" else "$index"
                val textLayoutResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                // Draw tiny rounded pill for node labels
                val pillWidth = textLayoutResult.size.width + 12f
                val pillHeight = textLayoutResult.size.height + 6f
                val pillTopLeft = Offset(point.x - pillWidth / 2f, point.y - 32f)

                drawRoundRect(
                    color = Color(0xFF1E1E24).copy(alpha = 0.9f),
                    topLeft = pillTopLeft,
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                )
                
                drawRoundRect(
                    color = nodeColor,
                    topLeft = pillTopLeft,
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                    style = Stroke(width = 1f)
                )

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(point.x - textLayoutResult.size.width / 2f, point.y - 29f)
                )
            }
        } else {
            // Draw default single pulsing home pin if no active route is calculated
            val center = Offset(width * 0.5f, height * 0.45f)
            drawCircle(
                color = originColor.copy(alpha = pulseAlpha),
                radius = 35f * pulseScale,
                center = center
            )
            drawCircle(
                color = originColor,
                radius = 12f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = center
            )
        }
    }
}
