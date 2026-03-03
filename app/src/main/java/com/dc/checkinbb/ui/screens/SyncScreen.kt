package com.dc.checkinbb.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dc.checkinbb.sync.SyncState
import com.dc.checkinbb.sync.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    onDismiss: () -> Unit,
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val syncState by syncViewModel.syncState.collectAsState()
    val connectedEndpoints by syncViewModel.connectedEndpoints.collectAsState()
    val isSuccess = syncState is SyncState.Success

    // Solicitar permisos de Nearby
    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            syncViewModel.startSession()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions.toTypedArray())
    }

    DisposableEffect(Unit) {
        onDispose { syncViewModel.stopSession() }
    }

    Scaffold(
        topBar = {
            if (!isSuccess) {
                TopAppBar(
                    title = { Text("Sincronizar", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            syncViewModel.stopSession()
                            onDismiss()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = isSuccess,
            transitionSpec = {
                fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.95f) togetherWith
                        fadeOut(tween(300))
            },
            label = "sync_content"
        ) { success ->
            if (success) {
                // ── Pantalla de éxito ─────────────────────────────────────
                val count = (syncState as? SyncState.Success)?.count ?: 0
                SuccessOverlay(
                    count = count,
                    onClose = {
                        syncViewModel.stopSession()
                        onDismiss()
                    }
                )
            } else {
                // ── UI normal de sincronización ───────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusCard(syncState = syncState)
                    if (connectedEndpoints.isNotEmpty()) {
                        DevicesCard(count = connectedEndpoints.size)
                    }
                    ActionCard(
                        recordCount = 0, // sin acceso al FeedingViewModel aquí
                        canSend = connectedEndpoints.isNotEmpty(),
                        onSend = { syncViewModel.sendRecords() }
                    )
                    HowItWorksCard()
                }
            }
        }
    }
}

// MARK: - Success Overlay

@Composable
private fun SuccessOverlay(count: Int, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        // Checkmark animado
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "checkmark_scale"
        )

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size((90 * scale).dp),
            tint = Color(0xFF4CAF50)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "¡Sincronización completada!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        if (count > 0) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color(0xFF4CAF50).copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "$count registro${if (count != 1) "s" else ""} fusionado${if (count != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        } else {
            Text(
                "Todo al día — sin cambios nuevos",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Puedes cerrar esta pantalla",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Icon(Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}

// MARK: - Status Card

@Composable
private fun StatusCard(syncState: SyncState) {
    val isAnimating = syncState is SyncState.Searching || syncState is SyncState.Syncing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = statusIcon(syncState),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = statusColor(syncState).let {
                    if (isAnimating) it.copy(alpha = pulseAlpha) else it
                }
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    statusTitle(syncState),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    statusSubtitle(syncState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// MARK: - Devices Card

@Composable
private fun DevicesCard(count: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color(0xFF4CAF50).copy(alpha = 0.12f)
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
            Column {
                Text(
                    "$count dispositivo${if (count != 1) "s" else ""} encontrado${if (count != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Listo para sincronizar ✓",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

// MARK: - Action Card

@Composable
private fun ActionCard(
    recordCount: Int,
    canSend: Boolean,
    onSend: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.Default.Upload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Enviar mis registros", fontWeight = FontWeight.Bold)
            }
            if (!canSend) {
                Text(
                    "Conecta con otro dispositivo Android primero",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// MARK: - How It Works Card

@Composable
private fun HowItWorksCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "CÓMO FUNCIONA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            StepRow(1, Icons.Default.PhoneAndroid, "Abre esta pantalla en ambos teléfonos")
            StepRow(2, Icons.Default.Wifi, "Se conectan solos por Wi-Fi o Bluetooth")
            StepRow(3, Icons.Default.Upload, "Uno de los dos envía sus registros")
            StepRow(4, Icons.Default.CheckCircle, "El otro recibe y combina los datos automáticamente")
        }
    }
}

@Composable
private fun StepRow(number: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Text(
                "$number",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// MARK: - Helpers

private fun statusIcon(state: SyncState) = when (state) {
    SyncState.Idle -> Icons.Default.PhoneAndroid
    SyncState.Searching -> Icons.Default.Search
    is SyncState.Connected -> Icons.Default.Link
    SyncState.Syncing -> Icons.Default.Sync
    is SyncState.Success -> Icons.Default.CheckCircle
    is SyncState.Failed -> Icons.Default.Error
}

@Composable
private fun statusColor(state: SyncState): Color = when (state) {
    SyncState.Idle -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    SyncState.Searching -> MaterialTheme.colorScheme.primary
    is SyncState.Connected -> Color(0xFF4CAF50)
    SyncState.Syncing -> Color(0xFFFF9800)
    is SyncState.Success -> Color(0xFF4CAF50)
    is SyncState.Failed -> MaterialTheme.colorScheme.error
}

private fun statusTitle(state: SyncState) = when (state) {
    SyncState.Idle -> "Listo para sincronizar"
    SyncState.Searching -> "Buscando dispositivos…"
    is SyncState.Connected -> "Conectado con ${state.deviceName}"
    SyncState.Syncing -> "Sincronizando…"
    is SyncState.Success -> "${state.count} registros sincronizados ✓"
    is SyncState.Failed -> "Error de conexión"
}

private fun statusSubtitle(state: SyncState) = when (state) {
    SyncState.Idle -> "Iniciando búsqueda"
    SyncState.Searching -> "Abre esta pantalla en el otro dispositivo"
    is SyncState.Connected -> "${state.deviceName} está listo. Presiona enviar."
    SyncState.Syncing -> "Transfiriendo datos…"
    is SyncState.Success -> "Los registros se han fusionado correctamente"
    is SyncState.Failed -> state.message
}
