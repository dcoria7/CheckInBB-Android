package com.dc.checkinbb.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedingDao {

    // --- Baby ---
    @Query("SELECT * FROM babies LIMIT 1")
    suspend fun getBaby(): BabyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaby(baby: BabyEntity)

    @Update
    suspend fun updateBaby(baby: BabyEntity)

    // --- Feedings ---
    @Query("SELECT * FROM feeding_records WHERE babyId = :babyId ORDER BY timestamp DESC")
    fun getFeedingsForBaby(babyId: String): Flow<List<FeedingRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeding(record: FeedingRecord)

    @Update
    suspend fun updateFeeding(record: FeedingRecord)

    @Delete
    suspend fun deleteFeeding(record: FeedingRecord)

    /** Usado por el widget: última toma sin Flow */
    @Query("SELECT * FROM feeding_records WHERE babyId = :babyId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestFeedingOnce(babyId: String): FeedingRecord?

    /** Usado por merge P2P: busca un registro específico por id */
    @Query("SELECT * FROM feeding_records WHERE id = :id LIMIT 1")
    suspend fun getFeedingById(id: String): FeedingRecord?

    /** Para P2P sync: snapshot de todos los registros (sin Flow) */
    @Query("SELECT * FROM feeding_records WHERE babyId = :babyId ORDER BY timestamp DESC")
    suspend fun getFeedingsOnce(babyId: String): List<FeedingRecord>
}
