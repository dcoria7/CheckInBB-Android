package com.dc.checkinbb.sync

import kotlinx.serialization.Serializable

@Serializable
data class FeedingRecordDTO(
    val id: String,
    val babyId: String,
    val timestamp: Long,
    val notes: String?,
    val lastModified: Long
)
