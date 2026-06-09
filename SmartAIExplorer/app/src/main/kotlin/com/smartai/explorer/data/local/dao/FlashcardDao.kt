package com.smartai.explorer.data.local.dao

import androidx.room.*
import com.smartai.explorer.data.local.entity.FlashcardEntity

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE documentId = :documentId")
    suspend fun getFlashcardsForDocument(documentId: String): List<FlashcardEntity>

    @Insert
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Query("DELETE FROM flashcards WHERE documentId = :documentId")
    suspend fun deleteFlashcardsForDocument(documentId: String)
}
