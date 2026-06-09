package com.smartai.explorer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartai.explorer.data.local.dao.*
import com.smartai.explorer.data.local.entity.*

@Database(
    entities = [
        DocumentEntity::class,
        ChatMessageEntity::class,
        SummaryEntity::class,
        FlashcardEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SmartAIDatabase : RoomDatabase() {
    abstract fun documentDao():    DocumentDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun summaryDao():     SummaryDao
    abstract fun flashcardDao():   FlashcardDao
}
