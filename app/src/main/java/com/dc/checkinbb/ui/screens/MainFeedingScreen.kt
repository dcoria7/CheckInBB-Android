package com.dc.checkinbb.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dc.checkinbb.data.local.FeedingRecord
import com.dc.checkinbb.ui.components.AnimatedBottleView
import com.dc.checkinbb.ui.components.FeedingDetailSheet
import com.dc.checkinbb.ui.components.FeedingListRow
import com.dc.checkinbb.ui.components.ManualEntryDialog
import com.dc.checkinbb.viewmodel.FeedingViewModel
import com.dc.checkinbb.viewmodel.ThemeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeedingScreen(
    feedingViewModel: FeedingViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val records by feedingViewModel.feedingRecords.collectAsState()
    val currentTimeMs by feedingViewModel.currentTime.collectAsState()
    val isLoading by feedingViewModel.isLoading.collectAsState()
    val baby by feedingViewModel.baby.collectAsState()

    var showManualEntry by remember { mutableStateOf(false) }
    var showFullHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSync by remember { mutableStateOf(false) }
    var selectedRecordForEdit by remember { mutableStateOf<FeedingRecord?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Map from record.id to the record pending deletion (waiting for undo window)
    val pendingDeletion = remember { mutableStateMapOf<String, FeedingRecord>() }

    fun requestDelete(record: FeedingRecord) {
        pendingDeletion[record.id] = record
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Toma eliminada",
                actionLabel = "DESHACER",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                // User tapped UNDO — restore
                pendingDeletion.remove(record.id)
            } else {
                // Timer expired — commit deletion
                pendingDeletion.remove(record.id)?.let { feedingViewModel.deleteFeeding(it) }
            }
        }
    }

    // Computations defined inline to trigger recomposition when state changes
    val timeSinceLast = records.firstOrNull()?.let { currentTimeMs - it.timestamp } ?: 0L
    val intervalMs = (baby.feedingIntervalHours * 3600 * 1000).toLong()
    val timeUntilNext = maxOf(0L, intervalMs - timeSinceLast)
    val progressValue = if (intervalMs == 0L) 0f else (timeSinceLast.toFloat() / intervalMs).coerceIn(0f, 1f)
    val hoursSince = timeSinceLast / 3_600_000.0
    val inWindow = hoursSince >= baby.feedingWindowMin && hoursSince <= baby.feedingWindowMax

    val formattedRemaining = run {
        val hours = (timeUntilNext / 3_600_000).toInt()
        val minutes = ((timeUntilNext % 3_600_000) / 60_000).toInt()
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "progress"
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🍼 ${baby.name}", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSync = true }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sincronizar")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Top Card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated bottle
                    AnimatedBottleView(
                        fillLevel = progressValue,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(width = 80.dp, height = 160.dp)
                    )

                    // Status text
                    if (timeUntilNext > 0) {
                        Text(
                            text = "Próxima en $formattedRemaining",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "¡Es hora de alimentar!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Red
                        )
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    brush = if (inWindow) {
                                        Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784)))
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    }
                                )
                        )
                    }

                    Text(
                        text = "Ventana: ${baby.feedingWindowMin.toInt()}h – ${baby.feedingWindowMax.toInt()}h",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Primary button
                    Button(
                        onClick = { feedingViewModel.registerFeeding() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inWindow) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "REGISTRAR TOMA",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    // Secondary button
                    OutlinedButton(
                        onClick = { showManualEntry = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Agregar manualmente")
                    }
                }
            }

            // ── History Card ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HISTORIAL",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${records.size} tomas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                    if (records.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay tomas registradas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        // Filter out records currently in the pending-delete window
                        val visibleRecords = records.filter { it.id !in pendingDeletion }
                        val displayRecords = if (showFullHistory) visibleRecords else visibleRecords.take(8)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            displayRecords.forEachIndexed { index, record ->
                                key(record.id) {
                                    SwipeToDismissRow(
                                        record = record,
                                        onDelete = { requestDelete(record) }
                                    ) {
                                        Box(modifier = Modifier.clickable { selectedRecordForEdit = record }) {
                                            FeedingListRow(
                                                record = record,
                                                currentTimeMs = currentTimeMs,
                                                isLatest = index == 0,
                                                intervalText = feedingViewModel.formattedIntervalSincePrevious(record)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (visibleRecords.size > 8) {
                            TextButton(
                                onClick = { showFullHistory = !showFullHistory },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (showFullHistory) "ver menos..." else "ver más...",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Sync status dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF4CAF50), RoundedCornerShape(50))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Local",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showManualEntry) {
        ManualEntryDialog(
            onDismiss = { showManualEntry = false },
            onConfirm = { timestamp ->
                feedingViewModel.registerFeeding(timestamp)
                showManualEntry = false
            }
        )
    }

    selectedRecordForEdit?.let { record ->
        FeedingDetailSheet(
            record = record,
            onDismiss = { selectedRecordForEdit = null },
            onSave = { newMs, notes ->
                feedingViewModel.updateFeedingDetails(record, newMs, notes)
                selectedRecordForEdit = null
            }
        )
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SettingsScreen(
                onDismiss = { showSettings = false },
                feedingViewModel = feedingViewModel,
                themeViewModel = themeViewModel
            )
        }
    }

    if (showSync) {
        ModalBottomSheet(
            onDismissRequest = { showSync = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SyncScreen(onDismiss = { showSync = false })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissRow(
    record: FeedingRecord,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    var deleted by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                deleted = true
                onDelete()
                true
            } else false
        }
    )

    // Immediately hide this composable after deletion so no red ghost bleeds into siblings
    if (deleted) return

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.dismissDirection != null
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = if (isSwiping) Color(0xFFE53935) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwiping) {
                    Text(
                        text = "🗑 Eliminar",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                content()
            }
        }
    )
}

