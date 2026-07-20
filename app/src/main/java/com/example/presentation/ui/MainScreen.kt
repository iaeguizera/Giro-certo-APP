package com.example.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.presentation.viewmodel.MainViewModel
import com.example.presentation.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to snackbar triggers in ViewModel
    LaunchedEffect(key1 = true) {
        viewModel.snackbarFlow.collectLatest { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Capture calculated route list for dynamic map representation
    val routeList = (uiState as? UiState.Success)?.route ?: emptyList()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Giro Certo!",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Text(
                            text = "Otimizador de Entregas TSP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA2A7B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF10121A), // Sleek night-mode title bar
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF10121A), // Seamless night-mode canvas
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // Root container placing Map as underlay context, and cards floating above
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. --- ADOÇÃO DO MAPA COMO CONTEXTO (Estilo Uber/Loggi) ---
            MapBackground(
                route = routeList,
                modifier = Modifier.fillMaxSize()
            )

            // 2. --- CARTÕES FLUTUANTES (Bottom Sheets/Floating Panel Scroll) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Viewport Spacer: Exposes the night-mode map background at the top,
                // allowing users to always see their real-time location/pins!
                Spacer(modifier = Modifier.height(180.dp))

                // Translucent floating inputs block
                InputSection(viewModel = viewModel)

                // State router with elegant card containers
                AnimatedContent(
                    targetState = uiState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                    },
                    label = "state_transition",
                    modifier = Modifier.fillMaxWidth()
                ) { state ->
                    when (state) {
                        is UiState.Idle -> {
                            EmptyResultPlaceholder()
                        }
                        is UiState.Loading -> {
                            InteractiveLoadingView()
                        }
                        is UiState.Success -> {
                            ResultSection(route = state.route, viewModel = viewModel)
                        }
                        is UiState.Error -> {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("error_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xEE2A1619), // Dark translucent error bg
                                    contentColor = Color(0xFFFFB4AB)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF93000A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF93000A).copy(alpha = 0.2f),
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFFFB4AB),
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Ops! Ocorreu um problema",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFFD9D4),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResultPlaceholder() {
    val cardBg = Color(0xEE1E1F28) // Translucent dark surface
    val cardBorderColor = Color(0xFF2E3245)
    val softLabelColor = Color(0xFFA2A7B8)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_placeholder_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2C3147),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = Color(0xFFFF9800), // Accent orange matching the primary theme focus
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Text(
                text = "Pronto para Otimizar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Insira o ponto de partida, adicione os destinos (via busca ou digitando) e toque em \"Otimizar Giro\" para traçar a melhor rota.",
                style = MaterialTheme.typography.bodyMedium,
                color = softLabelColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun InteractiveLoadingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val cardBg = Color(0xEE1E1F28)
    val cardBorderColor = Color(0xFF2E3245)
    val softLabelColor = Color(0xFFA2A7B8)
    val orangeCtaColor = Color(0xFFFF9800)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("loading_view"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer(
                        scaleX = pulseScale,
                        scaleY = pulseScale,
                        alpha = pulseAlpha
                    )
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2C3147),
                    modifier = Modifier.fillMaxSize()
                ) {}
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = orangeCtaColor,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Calculando Rota Ótima",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Acessando matriz OSRM e executando solucionador TSP...",
                    style = MaterialTheme.typography.bodySmall,
                    color = softLabelColor,
                    textAlign = TextAlign.Center
                )
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = orangeCtaColor,
                trackColor = Color(0xFF2C3147)
            )
        }
    }
}
