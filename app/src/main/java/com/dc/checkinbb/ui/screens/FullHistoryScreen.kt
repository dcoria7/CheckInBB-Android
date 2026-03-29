package com.dc.checkinbb.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import com.dc.checkinbb.data.local.FeedingRecord
import com.dc.checkinbb.ui.components.FeedingDetailSheet
import com.dc.checkinbb.ui.components.FeedingListRow
import com.dc.checkinbb.ui.components.SwipeToDismissFeedingRow
import com.dc.checkinbb.viewmodel.FeedingViewModel
import com.dc.checkinbb.viewmodel.HistoryStats
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun startOfDayMillis(timestamp: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = timestamp
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/** Yesterday at 00:00 local, aligned with iOS Calendar date math. */
private fun startOfYesterdayMillis(nowMs: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = startOfDayMillis(nowMs)
    c.add(Calendar.DAY_OF_YEAR, -1)
    return c.timeInMillis
}

/** Same fields iOS uses: formattedTime + formattedDate (medium) + notes. */
private fun recordFormattedTime(record: FeedingRecord): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(record.timestamp))

private fun recordFormattedDateMedium(record: FeedingRecord): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(record.timestamp))

private fun recordFormattedTime24h(record: FeedingRecord): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.timestamp))

private fun FeedingRecord.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    val q = query.trim()
    return recordFormattedTime(this).contains(q, ignoreCase = true) ||
        recordFormattedTime24h(this).contains(q, ignoreCase = true) ||
        recordFormattedDateMedium(this).contains(q, ignoreCase = true) ||
        (notes?.contains(q, ignoreCase = true) == true)
}

private fun dateHeaderLabel(dayStartMs: Long, nowMs: Long): String {
    val todayStart = startOfDayMillis(nowMs)
    if (dayStartMs == todayStart) return "Hoy"
    if (dayStartMs == startOfYesterdayMillis(nowMs)) return "Ayer"
    return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        .format(Date(dayStartMs))
}

private fun groupRecordsByDay(records: List<FeedingRecord>): List<Pair<Long, List<FeedingRecord>>> {
    val map = linkedMapOf<Long, MutableList<FeedingRecord>>()
    for (r in records) {
        val key = startOfDayMillis(r.timestamp)
        map.getOrPut(key) { mutableListOf() }.add(r)
    }
    return map.entries
        .sortedByDescending { it.key }
        .map { (dayStart, list) ->
            dayStart to list.sortedByDescending { it.timestamp }
        }
}

@Composable
private fun StatsSection(stats: HistoryStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                StatCard(
                    icon = Icons.Default.List,
                    value = stats.totalCount.toString(),
                    label = "TOTAL"
                )
            }
            item {
                StatCard(
                    icon = Icons.Default.CalendarToday,
                    value = stats.todayCount.toString(),
                    label = "HOY"
                )
            }
            item {
                StatCard(
                    icon = Icons.Default.ShowChart,
                    value = stats.averageIntervalHours?.let {
                        String.format(Locale.US, "%.1fh", it)
                    } ?: "--",
                    label = "PROMEDIO"
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = Modifier
            .widthIn(min = 104.dp)
            .width(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHistoryScreen(
    feedingViewModel: FeedingViewModel,
    onClose: () -> Unit
) {
    val records by feedingViewModel.feedingRecords.collectAsState()
    val currentTimeMs by feedingViewModel.currentTime.collectAsState()
    val stats = remember(records, currentTimeMs) {
        feedingViewModel.historyStats(currentTimeMs)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRecordForEdit by remember { mutableStateOf<FeedingRecord?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
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
                pendingDeletion.remove(record.id)
            } else {
                pendingDeletion.remove(record.id)?.let { feedingViewModel.deleteFeeding(it) }
            }
        }
    }

    val filtered = records
        .filter { it.matchesSearch(searchQuery) }
        .filter { it.id !in pendingDeletion }
    val grouped = remember(filtered) { groupRecordsByDay(filtered) }
    val todayStart = remember(currentTimeMs) { startOfDayMillis(currentTimeMs) }

    BackHandler(onBack = onClose)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Historial Completo",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar"
                        )
                    }
                }
            )
        },
        bottomBar = {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por hora o notas…") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                StatsSection(stats = stats)
            }

            if (filtered.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = if (searchQuery.isEmpty()) {
                                Icons.Outlined.Inbox
                            } else {
                                Icons.Default.Search
                            },
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                        Text(
                            text = if (records.isEmpty()) {
                                "No hay tomas registradas"
                            } else {
                                "No se encontraron resultados"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                for ((dayStart, dayRecords) in grouped) {
                    item(key = "header_$dayStart") {
                        AssistChip(
                            onClick = { },
                            enabled = false,
                            label = {
                                Text(
                                    text = dateHeaderLabel(dayStart, currentTimeMs),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                disabledLabelColor = MaterialTheme.colorScheme.primary,
                                disabledLeadingIconContentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    itemsIndexed(
                        items = dayRecords,
                        key = { _, r -> r.id }
                    ) { indexInDay, record ->
                        // iOS: isLatest = index == 0 && searchText.isEmpty && isDateInToday(record)
                        val isLatestRow = searchQuery.isBlank() &&
                            dayStart == todayStart &&
                            indexInDay == 0
                        SwipeToDismissFeedingRow(
                            record = record,
                            onDelete = { requestDelete(record) }
                        ) {
                            Box(
                                modifier = Modifier.clickable { selectedRecordForEdit = record }
                            ) {
                                FeedingListRow(
                                    record = record,
                                    currentTimeMs = currentTimeMs,
                                    isLatest = isLatestRow,
                                    intervalText = feedingViewModel.formattedIntervalSincePrevious(
                                        record
                                    ),
                                    use12HourTime = true,
                                    showDateUnderTime = false
                                )
                            }
                        }
                    }
                }
            }
        }
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
}
