package com.smartai.explorer.data.local.entity

import androidx.room.Entity
import com.smartai.explorer.domain.model.Summary
import com.smartai.explorer.domain.model.SummaryMode

@Entity(tableName = "summaries", primaryKeys = ["documentId", "mode"])
data class SummaryEntity(
    val documentId: String,
    val mode:       String,
    val content:    String,
    val cachedAt:   Long = System.currentTimeMillis(),
)

fun SummaryEntity.toDomain() = Summary(documentId, SummaryMode.valueOf(mode), content, cachedAt)
fun Summary.toEntity() = SummaryEntity(documentId, mode.name, content, cachedAt)
