package com.dc.checkinbb.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "feeding_records",
    foreignKeys = [
        ForeignKey(
            entity = BabyEntity::class,
            parentColumns = ["id"],
            childColumns = ["babyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("babyId")]
)
data class FeedingRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val babyId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
)
