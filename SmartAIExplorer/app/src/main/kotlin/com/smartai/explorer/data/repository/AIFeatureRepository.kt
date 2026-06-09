package com.smartai.explorer.data.repository

import com.smartai.explorer.data.local.dao.FlashcardDao
import com.smartai.explorer.data.local.dao.SummaryDao
import com.smartai.explorer.data.local.entity.FlashcardEntity
import com.smartai.explorer.data.local.entity.SummaryEntity
import com.smartai.explorer.data.local.entity.toDomain
import com.smartai.explorer.data.remote.SmartAIApiService
import com.smartai.explorer.data.remote.dto.*
import com.smartai.explorer.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIFeatureRepository @Inject constructor(
    private val api:         SmartAIApiService,
    private val summaryDao:  SummaryDao,
    private val flashcardDao: FlashcardDao,
) {
    suspend fun getSummary(documentId: String, mode: SummaryMode, opts: SummarizeRequest): Summary {
        if (mode != SummaryMode.CUSTOM) {
            summaryDao.getSummary(documentId, mode.name)?.let { return it.toDomain() }
        }
        val response = api.summarize(documentId, opts)
        if (mode != SummaryMode.CUSTOM) {
            summaryDao.insertSummary(SummaryEntity(documentId, mode.name, response.summary))
        }
        return Summary(documentId, mode, response.summary, System.currentTimeMillis())
    }

    suspend fun getFlashcards(documentId: String, count: Int = 10): List<Flashcard> {
        flashcardDao.getFlashcardsForDocument(documentId).let { cached ->
            if (cached.isNotEmpty()) return cached.map { it.toDomain() }
        }
        val response = api.generateFlashcards(documentId, FlashcardsRequest(count))
        val entities = response.flashcards.map {
            FlashcardEntity(
                documentId = documentId,
                front      = it.front,
                back       = it.back,
                difficulty = it.difficulty.uppercase(),
                page       = it.page,
            )
        }
        flashcardDao.insertFlashcards(entities)
        return flashcardDao.getFlashcardsForDocument(documentId).map { it.toDomain() }
    }

    suspend fun getInsights(documentId: String): DocumentInsights {
        val r = api.getInsights(documentId)
        return DocumentInsights(
            keyPoints   = r.keyPoints,
            entities    = r.entities.map {
                NamedEntity(
                    it.name,
                    runCatching { EntityType.valueOf(it.type.uppercase()) }.getOrDefault(EntityType.OTHER),
                )
            },
            actionItems = r.actionItems,
            topics      = r.topics,
        )
    }

    suspend fun getIndex(documentId: String): DocumentIndex {
        val r = api.getIndex(documentId)
        return DocumentIndex(
            documentId = documentId,
            sections   = r.sections.map { IndexSection(it.title, it.page, it.summary, it.level) },
        )
    }

    suspend fun explain(documentId: String, selectedText: String, mode: ExplainMode): ExplainResult {
        val r = api.explain(documentId, ExplainRequest(selectedText, mode.name.lowercase()))
        return ExplainResult(selectedText, mode, r.explanation)
    }
}
