package com.dc.checkinbb.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "babies")
data class BabyEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "Mi Bebé",
    val feedingIntervalHours: Double = 3.0,
    val feedingWindowMin: Double = 2.0,
    val feedingWindowMax: Double = 4.0
)
