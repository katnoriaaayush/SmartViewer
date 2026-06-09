package com.smartai.explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartai.explorer.domain.model.Difficulty
import com.smartai.explorer.domain.model.Flashcard

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: String,
    val front:      String,
    val back:       String,
    val difficulty: String,
    val page:       Int?,
)

fun FlashcardEntity.toDomain() =
    Flashcard(id, front, back, Difficulty.valueOf(difficulty), page)

fun Flashcard.toEntity(documentId: String) =
    FlashcardEntity(id, documentId, front, back, difficulty.name, page)
