package com.dc.checkinbb.data

import com.dc.checkinbb.data.local.BabyEntity
import com.dc.checkinbb.data.local.FeedingDao
import com.dc.checkinbb.data.local.FeedingRecord
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedingRepository @Inject constructor(
    private val dao: FeedingDao
) {
    suspend fun getOrCreateBaby(): BabyEntity {
        return dao.getBaby() ?: BabyEntity().also { dao.insertBaby(it) }
    }

    suspend fun updateBaby(baby: BabyEntity) {
        dao.updateBaby(baby)
    }

    fun getFeedingsFlow(babyId: String): Flow<List<FeedingRecord>> =
        dao.getFeedingsForBaby(babyId)

    suspend fun registerFeeding(babyId: String, timestamp: Long = System.currentTimeMillis(), notes: String? = null) {
        val record = FeedingRecord(
            id = UUID.randomUUID().toString(),
            babyId = babyId,
            timestamp = timestamp,
            notes = notes,
            lastModified = System.currentTimeMillis()
        )
        dao.insertFeeding(record)
    }

    suspend fun deleteFeeding(record: FeedingRecord) {
        dao.deleteFeeding(record)
    }

    suspend fun updateFeeding(record: FeedingRecord) {
        dao.updateFeeding(record.copy(lastModified = System.currentTimeMillis()))
    }

    /** Para P2P sync: snapshot de todos los registros sin flow */
    suspend fun getFeedingsOnce(babyId: String): List<FeedingRecord> =
        dao.getFeedingsOnce(babyId)
}
