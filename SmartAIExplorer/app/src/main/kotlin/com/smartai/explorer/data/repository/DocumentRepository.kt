package com.smartai.explorer.data.repository

import com.smartai.explorer.data.local.dao.DocumentDao
import com.smartai.explorer.data.local.entity.DocumentEntity
import com.smartai.explorer.data.local.entity.toDomain
import com.smartai.explorer.data.remote.SmartAIApiService
import com.smartai.explorer.domain.model.SmartDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val api: SmartAIApiService,
    private val dao: DocumentDao,
) {
    val documents: Flow<List<SmartDocument>> =
        dao.getAllDocuments().map { it.map { e -> e.toDomain() } }

    suspend fun uploadAndCache(filePart: MultipartBody.Part, localFileUri: String): SmartDocument {
        val response = api.uploadDocument(filePart)
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
