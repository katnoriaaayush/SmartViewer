package com.smartai.explorer.data.repository

import com.smartai.explorer.data.local.dao.DocumentDao
import com.smartai.explorer.data.local.entity.DocumentEntity
import com.smartai.explorer.data.local.entity.toDomain
import com.smartai.explorer.data.remote.SmartAIApiService
import com.smartai.explorer.domain.model.SmartDocument
import com.smartai.explorer.util.AppLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DocumentRepo"

@Singleton
class DocumentRepository @Inject constructor(
    private val api: SmartAIApiService,
    private val dao: DocumentDao,
) {
    val documents: Flow<List<SmartDocument>> =
        dao.getAllDocuments().map { it.map { e -> e.toDomain() } }

    suspend fun uploadAndCache(filePart: MultipartBody.Part, localFileUri: String): SmartDocument {
        AppLog.i(TAG, "Uploading document to server…")
        val response = api.uploadDocument(filePart)
        AppLog.i(TAG, "Upload OK → documentId=${response.documentId} sessionId=${response.sessionId}")
        val entity = DocumentEntity(
            documentId   = response.documentId,
            fileName     = response.fileName,
            mimeType     = response.mimeType,
            fileSize     = response.fileSize,
            sessionId    = response.sessionId,
            localFileUri = localFileUri,
        )
        dao.insertDocument(entity)
        return entity.toDomain()
    }

    suspend fun getDocument(id: String): SmartDocument? =
        dao.getDocument(id)?.toDomain()
}
