package com.example.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RouteResult
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.StopDeliveryState
import com.example.utils.WazeIntentHelper

@Composable
fun ResultSection(
    route: List<RouteResult>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val deliveryStates by viewModel.deliveryStates.collectAsState()
    val activeStopOrder by viewModel.activeStopOrder.collectAsState()

    // Aggregate statistics
    val totalStops = route.size
    val deliveredCount = deliveryStates.values.count { it.status == "ENTREGUE" }
    val totalDistance = route.lastOrNull()?.accumulatedDistance ?: 0.0

    // Find the currently active stop
    val activeStop = route.find { it.order == activeStopOrder } ?: route.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. --- STATS PILL HEADER BAR (As shown in screenshot top bar) ---
        StatsPillHeader(
            deliveredCount = deliveredCount,
            totalStops = totalStops,
            totalDistance = totalDistance
        )

        // 2. --- ACTIVE STOP / DELIVERY FICHA CARD BLOCK (Design de Cartão de Entrega Ativa) ---
        activeStop?.let { stop ->
            val stopState = deliveryStates[stop.order] ?: StopDeliveryState()
            ActiveDeliveryCard(
                stop = stop,
                stopState = stopState,
                onUpdateStatus = { status -> viewModel.updateStopStatus(stop.order, status) },
                onUpdateDetails = { comp, b -> viewModel.updateStopDetails(stop.order, comp, b) },
                onNavigateWaze = {
                    WazeIntentHelper.openWaze(context, stop.coordinate.lat, stop.coordinate.lng)
                },
                onNavigateGoogleMaps = {
                    WazeIntentHelper.openGoogleMaps(context, stop.coordinate.lat, stop.coordinate.lng)
                }
            )
        }

        // 3. --- CONTROL ROW (Copiar Ordem & Rota Completa) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val textToCopy = buildAnnotatedString {
                        route.forEach { stop ->
                            val state = deliveryStates[stop.order] ?: StopDeliveryState()
                            val statusSuffix = when (state.status) {
                                "ENTREGUE" -> " [ENTREGUE]"
                                else -> " [PENDENTE]"
                            }
                            val suffix = if (stop.isOrigin) " [PARTIDA]" else ""
                            append("${stop.order}. ${stop.addressText}$suffix$statusSuffix\n")
                        }
                    }
                    clipboardManager.setText(AnnotatedString(textToCopy.text))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("copy_order_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copiar Roteiro", style = MaterialTheme.typography.labelMedium)
            }

            Button(
                onClick = {
                    WazeIntentHelper.openCompleteRoute(
                        context,
                        route.map { it.coordinate }
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("open_route_maps_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Rota Completa", style = MaterialTheme.typography.labelMedium)
            }
        }

        // 4. --- LISTA COMPLETA DE PARADAS ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Lista Completa de Paradas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )

            route.forEach { stop ->
                val stopState = deliveryStates[stop.order] ?: StopDeliveryState()
                val isSelected = stop.order == activeStopOrder

                StopListItemRow(
                    stop = stop,
                    stopState = stopState,
                    isSelected = isSelected,
                    onClick = { viewModel.selectActiveStop(stop.order) }
                )
            }
        }
    }
}

/**
 * High-fidelity representation of the dark stats overlay pill shown at the top of the map.
 */
@Composable
private fun StatsPillHeader(
    deliveredCount: Int,
    totalStops: Int,
    totalDistance: Double
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stats_pill_header"),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1E1E24), // Near-black elegant dark background
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Delivered stat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Entregue",
                    tint = Color(0xFF4CAF50), // Vibrant Green
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$deliveredCount/$totalStops",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Divider
            Box(modifier = Modifier.height(16.dp).width(1.dp).background(Color.White.copy(alpha = 0.3f)))

            // Distance/Progress stat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Distância",
                    tint = Color(0xFF2196F3), // Elegant Blue
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = String.format("%.1f km", totalDistance),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Premium Ficha de Entrega card representation, styled in custom grouped sections exactly as the user's screenshot.
 */
@Composable
private fun ActiveDeliveryCard(
    stop: RouteResult,
    stopState: StopDeliveryState,
    onUpdateStatus: (String) -> Unit,
    onUpdateDetails: (String, String) -> Unit,
    onNavigateWaze: () -> Unit,
    onNavigateGoogleMaps: () -> Unit
) {
    var isEditingDetails by remember(stop.order) { mutableStateOf(false) }
    var complementInput by remember(stop.order) { mutableStateOf(stopState.complement) }
    var bairroInput by remember(stop.order) { mutableStateOf(stopState.bairro) }

    var navigationMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_delivery_card_${stop.order}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section 1: Address and Pencil Icon Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar detalhes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { isEditingDetails = !isEditingDetails }
                    )
                    Text(
                        text = stop.addressText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { isEditingDetails = !isEditingDetails },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações do Item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Section 2: Complement Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Complemento:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isEditingDetails) {
                    OutlinedTextField(
                        value = complementInput,
                        onValueChange = { complementInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                } else {
                    Text(
                        text = stopState.complement.ifEmpty { "Sem complemento especificado" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stopState.complement.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Section 3: Action Buttons (Navegar, Lista, Entregue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Button 1: Navegar
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { navigationMenuExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("navigate_action_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D6EFD), // Dynamic iOS/M3 Blue
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Navegar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = navigationMenuExpanded,
                        onDismissRequest = { navigationMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Abrir no Waze", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF00D4B2)) },
                            onClick = {
                                navigationMenuExpanded = false
                                onNavigateWaze()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Abrir no Google Maps", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF4CAF50)) },
                            onClick = {
                                navigationMenuExpanded = false
                                onNavigateGoogleMaps()
                            }
                        )
                    }
                }

                // Button 2: Lista (indicates total list / informational)
                OutlinedButton(
                    onClick = { /* Informational or toggling active */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lista", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                // Button 3: Entregue
                Button(
                    onClick = { onUpdateStatus("ENTREGUE") },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("success_delivery_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF198754), // Safe Success Green
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Entregue", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            // Section 5: Neighborhood (Bairro) Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Bairro:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (isEditingDetails) {
                    OutlinedTextField(
                        value = bairroInput,
                        onValueChange = { bairroInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            isEditingDetails = false
                            onUpdateDetails(complementInput, bairroInput)
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Salvar")
                    }
                } else {
                    Text(
                        text = stopState.bairro,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean representational list item row that supports statuses, order badge, active selection.
 */
@Composable
private fun StopListItemRow(
    stop: RouteResult,
    stopState: StopDeliveryState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "listItemBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("stop_item_row_${stop.order}"),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sequence indicator
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = when {
                    stop.isOrigin -> MaterialTheme.colorScheme.primary
                    stopState.status == "ENTREGUE" -> Color(0xFF198754)
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = when {
                    stop.isOrigin -> MaterialTheme.colorScheme.onPrimary
                    stopState.status == "ENTREGUE" -> Color.White
                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${stop.order}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Description block
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (stop.isOrigin) "Ponto de Partida" else "Parada ${stop.order - 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (stop.isOrigin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (stopState.status) {
                            "ENTREGUE" -> Color(0xFFE8F5E9)
                            else -> Color(0xFFECEFF1)
                        },
                        contentColor = when (stopState.status) {
                            "ENTREGUE" -> Color(0xFF198754)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ) {
                        Text(
                            text = when (stopState.status) {
                                "ENTREGUE" -> "Entregue"
                                else -> "Pendente"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = stop.addressText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!stop.isOrigin) {
                    Text(
                        text = "Bairro: ${stopState.bairro}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
