package com.dc.checkinbb.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dc.checkinbb.data.FeedingRepository
import com.dc.checkinbb.data.local.FeedingRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val nearbyManager: NearbyConnectionsManager,
    private val repository: FeedingRepository
) : ViewModel() {

    val syncState: StateFlow<SyncState> = nearbyManager.syncState
    val connectedEndpoints: StateFlow<List<String>> = nearbyManager.connectedEndpoints

    fun startSession() = nearbyManager.startSession()
    fun stopSession() = nearbyManager.stopSession()

    fun sendRecords() {
        viewModelScope.launch {
            val baby = repository.getOrCreateBaby()
            val records = repository.getFeedingsOnce(baby.id)
            val dtos = records.map { it.toDTO() }
            nearbyManager.sendFeedings(dtos)
        }
    }

    private fun FeedingRecord.toDTO() = FeedingRecordDTO(
        id = id,
        babyId = babyId,
        timestamp = timestamp,
        notes = notes,
        lastModified = lastModified
    )
}
