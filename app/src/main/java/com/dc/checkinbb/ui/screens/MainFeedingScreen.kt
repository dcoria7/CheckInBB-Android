package com.dc.checkinbb.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.dc.checkinbb.data.local.FeedingRecord
import com.dc.checkinbb.ui.components.AnimatedBottleView
import com.dc.checkinbb.ui.components.FeedingDetailSheet
import com.dc.checkinbb.ui.components.ToastBanner
import com.dc.checkinbb.ui.components.ToastBannerStyle
import com.dc.checkinbb.ui.components.FeedingListRow
import com.dc.checkinbb.ui.components.ManualEntryDialog
import com.dc.checkinbb.ui.components.SwipeToDismissFeedingRow
import com.dc.checkinbb.viewmodel.FeedingViewModel
import com.dc.checkinbb.viewmodel.ThemeViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeedingScreen(
    feedingViewModel: FeedingViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    onOpenFullHistory: () -> Unit = {}
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

    var showingToast by remember { mutableStateOf(false) }
    var toastText by remember { mutableStateOf("") }
    var toastStyle by remember { mutableStateOf(ToastBannerStyle.Info) }
    var toastDismissJob by remember { mutableStateOf<Job?>(null) }

    var isRegisterCooldownActive by remember { mutableStateOf(false) }
    var cooldownRecordId by remember { mutableStateOf<String?>(null) }
    var cooldownJob by remember { mutableStateOf<Job?>(null) }

    fun cancelRegisterCooldown() {
        cooldownJob?.cancel()
        cooldownJob = null
        cooldownRecordId = null
        isRegisterCooldownActive = false
    }

    LaunchedEffect(records.firstOrNull()?.id, isRegisterCooldownActive, cooldownRecordId) {
        val latestId = records.firstOrNull()?.id ?: return@LaunchedEffect
        if (isRegisterCooldownActive && cooldownRecordId != null && latestId != cooldownRecordId) {
            cancelRegisterCooldown()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            toastDismissJob?.cancel()
            cooldownJob?.cancel()
        }
    }

    fun showToastBanner(text: String, style: ToastBannerStyle) {
        toastDismissJob?.cancel()
        toastText = text
        toastStyle = style
        showingToast = true
        toastDismissJob = scope.launch {
            delay(1800)
            showingToast = false
        }
    }

    fun startRegisterCooldown(latestRecordId: String?) {
        cooldownJob?.cancel()
        cooldownRecordId = latestRecordId
        isRegisterCooldownActive = true
        cooldownJob = scope.launch {
            delay(60_000)
            cancelRegisterCooldown()
        }
    }

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
    val isOverdue = timeUntilNext <= 0L

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                    // Animated bottle (size + overdue pulse aligned with iOS)
                    AnimatedBottleView(
                        fillLevel = progressValue,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.secondary,
                        isOverdue = isOverdue,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .size(width = 100.dp, height = 180.dp)
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
                        onClick = {
                            scope.launch {
                                val newId = feedingViewModel.registerFeeding()
                                if (newId != null) {
                                    showToastBanner("Toma registrada", ToastBannerStyle.Success)
                                    startRegisterCooldown(newId)
                                } else {
                                    showToastBanner(
                                        "No se pudo registrar la toma",
                                        ToastBannerStyle.Error
                                    )
                                }
                            }
                        },
                        enabled = !isLoading && !isRegisterCooldownActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inWindow) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                            disabledContainerColor = if (inWindow) {
                                Color(0xFF4CAF50).copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            },
                            disabledContentColor = Color.White.copy(alpha = 0.85f)
                        )
                    ) {
                        when {
                            isLoading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            isRegisterCooldownActive -> {
                                Text(
                                    text = "Registrada…",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            else -> {
                                Text(
                                    text = "REGISTRAR TOMA",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
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
                                    SwipeToDismissFeedingRow(
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

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
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
                            if (visibleRecords.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onOpenFullHistory,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ver historial completo")
                                }
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

            AnimatedVisibility(
                visible = showingToast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .zIndex(10f),
                enter = slideInVertically { -it } + fadeIn(
                    animationSpec = spring(dampingRatio = 0.85f)
                ),
                exit = slideOutVertically { -it } + fadeOut(animationSpec = tween(250))
            ) {
                ToastBanner(text = toastText, style = toastStyle)
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
