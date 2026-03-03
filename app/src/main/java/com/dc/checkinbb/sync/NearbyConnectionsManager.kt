package com.dc.checkinbb.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.dc.checkinbb.data.local.AppDatabase
import com.dc.checkinbb.data.local.FeedingRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class SyncState {
    object Idle : SyncState()
    object Searching : SyncState()
    data class Connected(val deviceName: String) : SyncState()
    object Syncing : SyncState()
    data class Success(val count: Int) : SyncState()
    data class Failed(val message: String) : SyncState()
}

class NearbyConnectionsManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "NearbySync"
        private const val SERVICE_ID = "com.dc.checkinbb.sync"
        private val STRATEGY = Strategy.P2P_STAR
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _connectedEndpoints = MutableStateFlow<List<String>>(emptyList())
    val connectedEndpoints: StateFlow<List<String>> = _connectedEndpoints

    private val endpointNames = mutableMapOf<String, String>()

    // MARK: - Advertising + Discovery

    fun startSession() {
        startAdvertising()
        startDiscovery()
        _syncState.value = SyncState.Searching
    }

    fun stopSession() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        _connectedEndpoints.value = emptyList()
        endpointNames.clear()
        _syncState.value = SyncState.Idle
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startAdvertising(
            android.os.Build.MODEL,
            SERVICE_ID,
            connectionLifecycleCallback,
            options
        ).addOnFailureListener {
            Log.e(TAG, "Advertising failed: ${it.message}")
            _syncState.value = SyncState.Failed("No se pudo anunciar el dispositivo")
        }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
            .addOnFailureListener {
                Log.e(TAG, "Discovery failed: ${it.message}")
            }
    }

    // MARK: - Sending

    fun sendFeedings(dtos: List<FeedingRecordDTO>) {
        val json = Json.encodeToString(dtos)
        val payload = Payload.fromBytes(json.toByteArray(Charsets.UTF_8))
        _syncState.value = SyncState.Syncing

        _connectedEndpoints.value.forEach { endpointId ->
            connectionsClient.sendPayload(endpointId, payload)
        }
    }

    // MARK: - Merge

    private fun handleIncomingPayload(bytes: ByteArray) {
        scope.launch {
            try {
                val json = bytes.toString(Charsets.UTF_8)
                val incoming = Json.decodeFromString<List<FeedingRecordDTO>>(json)
                val count = merge(incoming)
                _syncState.value = SyncState.Success(count)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing payload: ${e.message}")
                _syncState.value = SyncState.Failed("Error al procesar los datos recibidos")
            }
        }
    }

    private suspend fun merge(incoming: List<FeedingRecordDTO>): Int {
        val db = AppDatabase.getInstance(context)
        val dao = db.feedingDao()
        val baby = dao.getBaby() ?: return 0

        var mergedCount = 0

        for (dto in incoming) {
            if (dto.babyId != baby.id) continue

            val existing = dao.getFeedingById(dto.id)
            if (existing == null) {
                // Nuevo registro
                dao.insertFeeding(
                    FeedingRecord(
                        id = dto.id,
                        babyId = dto.babyId,
                        timestamp = dto.timestamp,
                        notes = dto.notes,
                        lastModified = dto.lastModified
                    )
                )
                mergedCount++
            } else if (dto.lastModified > existing.lastModified) {
                // Registro más reciente gana
                dao.updateFeeding(existing.copy(
                    timestamp = dto.timestamp,
                    notes = dto.notes,
                    lastModified = dto.lastModified
                ))
                mergedCount++
            }
        }

        return mergedCount
    }

    // MARK: - Callbacks

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Aceptar automáticamente
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            endpointNames[endpointId] = info.endpointName
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                val name = endpointNames[endpointId] ?: endpointId
                _connectedEndpoints.value = _connectedEndpoints.value + endpointId
                _syncState.value = SyncState.Connected(name)
                Log.i(TAG, "Connected to $name")
            } else {
                endpointNames.remove(endpointId)
                Log.e(TAG, "Connection failed to $endpointId")
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpoints.value = _connectedEndpoints.value - endpointId
            endpointNames.remove(endpointId)
            if (_connectedEndpoints.value.isEmpty() &&
                _syncState.value !is SyncState.Success
            ) {
                _syncState.value = SyncState.Searching
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.i(TAG, "Found endpoint: ${info.endpointName}")
            connectionsClient.requestConnection(
                android.os.Build.MODEL,
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            Log.i(TAG, "Lost endpoint: $endpointId")
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { handleIncomingPayload(it) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS &&
                _syncState.value is SyncState.Syncing
            ) {
                // El sender también recibe SUCCESS cuando el transfer termina
                _syncState.value = SyncState.Success(0)
            }
        }
    }
}
