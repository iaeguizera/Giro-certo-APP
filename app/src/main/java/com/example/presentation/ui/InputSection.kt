package com.example.presentation.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.presentation.viewmodel.MainViewModel

@Composable
fun InputSection(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val origin by viewModel.originInput.collectAsState()
    val destinations by viewModel.destinationsInput.collectAsState()
    val isExact by viewModel.isExactMode.collectAsState()

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val csvContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                viewModel.importCsv(csvContent)
            } catch (e: Exception) {
                // Fail silently
            }
        }
    }

    // Modern translucent card styling for floating feeling
    val cardBg = Color(0xEE1E1F28) // Translucent dark surface
    val cardBorderColor = Color(0xFF2E3245) // Sharp thin border
    val softLabelColor = Color(0xFFA2A7B8) // Softened secondary text color
    val orangeCtaColor = Color(0xFFFF9800) // Vibrant CTA Orange (Rappi/FedEx style)
    val secondaryActionButtonColor = Color(0xFF2C3147) // Soft background for other actions

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- BLOC 1: PONTO DE PARTIDA ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("origin_block_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = orangeCtaColor, // Highlight start/destination visually
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Ponto de Partida",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = cardBorderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { viewModel.originInput.value = it },
                        placeholder = { Text("Endereço de partida ou coordenadas GPS") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = softLabelColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (origin.isNotEmpty()) {
                                IconButton(onClick = { viewModel.originInput.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Limpar",
                                        tint = softLabelColor
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("origin_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = orangeCtaColor,
                            unfocusedBorderColor = cardBorderColor,
                            focusedContainerColor = Color(0xFF14151D),
                            unfocusedContainerColor = Color(0xFF14151D),
                            focusedPlaceholderColor = softLabelColor,
                            unfocusedPlaceholderColor = softLabelColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    IconButton(
                        onClick = { viewModel.fetchCurrentLocationAsOrigin(context) },
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("location_button"),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = secondaryActionButtonColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Minha Localização",
                            tint = orangeCtaColor
                        )
                    }
                }

                // Origin Suggestions List
                val originSuggestions by viewModel.originSuggestions.collectAsState()
                if (originSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181A24)),
                        border = BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column {
                            originSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = Color.White) },
                                    onClick = { viewModel.selectOriginSuggestion(suggestion) }
                                )
                                HorizontalDivider(color = cardBorderColor)
                            }
                        }
                    }
                }
            }
        }

        // --- BLOC 2: BUSCAR E ADICIONAR DESTINO ---
        val destSearch by viewModel.destinationSearchInput.collectAsState()
        val destSuggestions by viewModel.destinationSuggestions.collectAsState()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_dest_block_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = softLabelColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Adicionar Destino",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = cardBorderColor)

                OutlinedTextField(
                    value = destSearch,
                    onValueChange = { viewModel.destinationSearchInput.value = it },
                    placeholder = { Text("Buscar endereço (Ex: Av. Paulista, 1000)") }, // Moved from long description text
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = softLabelColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = orangeCtaColor,
                        unfocusedBorderColor = cardBorderColor,
                        focusedContainerColor = Color(0xFF14151D),
                        unfocusedContainerColor = Color(0xFF14151D),
                        focusedPlaceholderColor = softLabelColor,
                        unfocusedPlaceholderColor = softLabelColor
                    ),
                    trailingIcon = {
                        if (destSearch.isNotEmpty()) {
                            IconButton(onClick = { viewModel.destinationSearchInput.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpar busca",
                                    tint = softLabelColor
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                )

                if (destSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF181A24)),
                        border = BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column {
                            destSuggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = { Text(suggestion, style = MaterialTheme.typography.bodyMedium, color = Color.White) },
                                    onClick = { viewModel.selectDestinationSuggestion(suggestion) }
                                )
                                HorizontalDivider(color = cardBorderColor)
                            }
                        }
                    }
                }
            }
        }

        // --- BLOC 3: DESTINOS ADICIONADOS ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dest_list_block_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = softLabelColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Lista de Destinos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = { csvLauncher.launch("text/*") },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("import_csv_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondaryActionButtonColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Importar CSV",
                            tint = orangeCtaColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Importar CSV", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider(color = cardBorderColor)

                OutlinedTextField(
                    value = destinations,
                    onValueChange = { viewModel.destinationsInput.value = it },
                    placeholder = { Text("Nenhum destino adicionado.\nBusque acima ou digite aqui (um por linha):\nAv. Paulista, 1000\nRua Augusta, 500") }, // Text condensed inside placeholder
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destinations_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = orangeCtaColor,
                        unfocusedBorderColor = cardBorderColor,
                        focusedContainerColor = Color(0xFF14151D),
                        unfocusedContainerColor = Color(0xFF14151D),
                        focusedPlaceholderColor = softLabelColor,
                        unfocusedPlaceholderColor = softLabelColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // --- BLOC 4: CONFIGURAÇÕES DE OTIMIZAÇÃO ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_block_card"),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = softLabelColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Preferências de Otimização",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = cardBorderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Otimização Exata (Força Bruta)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "Melhor rota absoluta (máximo de 10 paradas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = softLabelColor
                        )
                    }
                    Switch(
                        checked = isExact,
                        onCheckedChange = { viewModel.isExactMode.value = it },
                        modifier = Modifier.testTag("exact_mode_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = orangeCtaColor,
                            checkedTrackColor = orangeCtaColor.copy(alpha = 0.4f),
                            uncheckedThumbColor = softLabelColor,
                            uncheckedTrackColor = cardBorderColor
                        )
                    )
                }
            }
        }

        // --- BLOC 5: AÇÕES PRINCIPAIS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.clearAllData() },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("clear_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = softLabelColor
                ),
                border = BorderStroke(1.dp, cardBorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = softLabelColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Limpar Tudo", style = MaterialTheme.typography.labelLarge)
            }

            Button(
                onClick = { viewModel.optimizeRoute() },
                modifier = Modifier
                    .weight(1.5f)
                    .height(52.dp)
                    .testTag("optimize_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = orangeCtaColor, // Orange exclusively for major Call to Action
                    contentColor = Color(0xFF10121A) // High contrast dark text for maximum readability
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Route, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Otimizar Giro", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
