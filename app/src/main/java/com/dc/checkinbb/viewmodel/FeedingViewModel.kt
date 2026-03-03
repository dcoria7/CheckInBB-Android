package com.dc.checkinbb.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dc.checkinbb.data.FeedingRepository
import com.dc.checkinbb.data.local.BabyEntity
import com.dc.checkinbb.data.local.FeedingRecord
import com.dc.checkinbb.widget.UpdateWidgetWorker
import com.dc.checkinbb.workers.FeedingNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class FeedingViewModel @Inject constructor(
    application: Application,
    private val repository: FeedingRepository,
    private val notificationManager: FeedingNotificationManager
) : AndroidViewModel(application) {

    private fun triggerWidgetUpdate() =
        UpdateWidgetWorker.enqueueOnce(getApplication())

    // --- Baby state ---
    private val _baby = MutableStateFlow(BabyEntity())
    val baby: StateFlow<BabyEntity> = _baby.asStateFlow()

    // --- Feedings ---
    private val _feedingRecords = MutableStateFlow<List<FeedingRecord>>(emptyList())
    val feedingRecords: StateFlow<List<FeedingRecord>> = _feedingRecords.asStateFlow()

    // --- Live clock (updates every 20s) ---
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    // --- Loading ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            val baby = repository.getOrCreateBaby()
            _baby.value = baby
            repository.getFeedingsFlow(baby.id).collect { records ->
                _feedingRecords.value = records
                rescheduleNotifications()
            }
        }
        startTimer()
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (true) {
                delay(20_000L)
                _currentTime.value = System.currentTimeMillis()
            }
        }
    }

    // --- Computed properties ---
    fun timeSinceLastFeeding(): Long {
        val last = _feedingRecords.value.firstOrNull() ?: return 0L
        return _currentTime.value - last.timestamp
    }

    fun timeUntilNextFeeding(): Long {
        val intervalMs = (_baby.value.feedingIntervalHours * 3600 * 1000).toLong()
        val remaining = intervalMs - timeSinceLastFeeding()
        return maxOf(0L, remaining)
    }

    fun progressBarValue(): Float {
        val intervalMs = (_baby.value.feedingIntervalHours * 3600 * 1000).toLong()
        if (intervalMs == 0L) return 0f
        return (timeSinceLastFeeding().toFloat() / intervalMs).coerceIn(0f, 1f)
    }

    fun isInFeedingWindow(): Boolean {
        val hours = timeSinceLastFeeding() / 3_600_000.0
        return hours >= _baby.value.feedingWindowMin && hours <= _baby.value.feedingWindowMax
    }

    fun formattedTimeRemaining(): String {
        val ms = timeUntilNextFeeding()
        val hours = (ms / 3_600_000).toInt()
        val minutes = ((ms % 3_600_000) / 60_000).toInt()
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    fun timeAgo(record: FeedingRecord): String {
        val ms = _currentTime.value - record.timestamp
        val hours = (ms / 3_600_000).toInt()
        val minutes = ((ms % 3_600_000) / 60_000).toInt()
        return "${hours}h ${minutes}m"
    }

    fun formattedIntervalSincePrevious(record: FeedingRecord): String? {
        val records = _feedingRecords.value
        val index = records.indexOfFirst { it.id == record.id }
        if (index < 0 || index >= records.size - 1) return null
        val ms = records[index].timestamp - records[index + 1].timestamp
        val hours = (ms / 3_600_000).toInt()
        val minutes = ((ms % 3_600_000) / 60_000).toInt()
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    // --- Actions ---
    fun registerFeeding() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.registerFeeding(_baby.value.id)
            _currentTime.value = System.currentTimeMillis()
            triggerWidgetUpdate()
            _isLoading.value = false
        }
    }

    fun registerFeeding(timestamp: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            val maxTime = System.currentTimeMillis()
            val safeTimestamp = if (timestamp > maxTime) maxTime else timestamp
            repository.registerFeeding(_baby.value.id, safeTimestamp)
            triggerWidgetUpdate()
            _isLoading.value = false
        }
    }

    fun deleteFeeding(record: FeedingRecord) {
        viewModelScope.launch {
            repository.deleteFeeding(record)
            triggerWidgetUpdate()
        }
    }

    fun updateNotes(record: FeedingRecord, notes: String) {
        viewModelScope.launch {
            repository.updateFeeding(record.copy(notes = notes.ifBlank { null }, lastModified = System.currentTimeMillis()))
        }
    }

    fun updateFeedingDetails(record: FeedingRecord, newTimestamp: Long, newNotes: String) {
        viewModelScope.launch {
            val maxTime = System.currentTimeMillis()
            val safeTimestamp = if (newTimestamp > maxTime) maxTime else newTimestamp
            repository.updateFeeding(
                record.copy(
                    timestamp = safeTimestamp,
                    notes = newNotes.takeIf { it.isNotBlank() },
                    lastModified = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateBabySettings(name: String, intervalHours: Double, windowMin: Double, windowMax: Double) {
        viewModelScope.launch {
            val trimmed = name.trim().ifBlank { "Mi Bebé" }
            val updated = _baby.value.copy(
                name = trimmed,
                feedingIntervalHours = intervalHours,
                feedingWindowMin = windowMin,
                feedingWindowMax = windowMax
            )
            repository.updateBaby(updated)
            _baby.value = updated
            rescheduleNotifications()
        }
    }

    private fun rescheduleNotifications() {
        val lastFeeding = _feedingRecords.value.firstOrNull()
        if (lastFeeding == null) {
            notificationManager.cancelAll()
            return
        }

        val timeSinceLastFeedingMs = System.currentTimeMillis() - lastFeeding.timestamp
        notificationManager.scheduleFeeding(
            intervalHours = _baby.value.feedingIntervalHours,
            windowMinHours = _baby.value.feedingWindowMin,
            windowMaxHours = _baby.value.feedingWindowMax,
            timeSinceLastFeedingMs = timeSinceLastFeedingMs
        )
    }
}
