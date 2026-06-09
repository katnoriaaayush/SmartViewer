package com.smartai.explorer.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartai.explorer.domain.model.SmartDocument

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val documentId:   String,
    val fileName:     String,
    val mimeType:     String,
    val fileSize:     Long,
    val sessionId:    String,
    val localFileUri: String,
    val uploadedAt:   Long = System.currentTimeMillis(),
)

fun DocumentEntity.toDomain() = SmartDocument(
    documentId, fileName, mimeType, fileSize, sessionId, localFileUri, uploadedAt,
)

fun SmartDocument.toEntity() = DocumentEntity(
    documentId, fileName, mimeType, fileSize, sessionId, localFileUri, uploadedAt,
)
