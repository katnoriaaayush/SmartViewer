package com.smartai.explorer.data.local.dao

import androidx.room.*
import com.smartai.explorer.data.local.entity.SummaryEntity

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE documentId = :documentId AND mode = :mode")
    suspend fun getSummary(documentId: String, mode: String): SummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)
}
