package com.smartai.explorer.data.repository

import com.smartai.explorer.data.local.dao.FlashcardDao
import com.smartai.explorer.data.local.dao.SummaryDao
import com.smartai.explorer.data.local.entity.FlashcardEntity
import com.smartai.explorer.data.local.entity.SummaryEntity
import com.smartai.explorer.data.local.entity.toDomain
import com.smartai.explorer.data.remote.SmartAIApiService
import com.smartai.explorer.data.remote.dto.*
import com.smartai.explorer.domain.model.*
import com.smartai.explorer.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AIFeatureRepo"

@Singleton
class AIFeatureRepository @Inject constructor(
    private val api:         SmartAIApiService,
    private val summaryDao:  SummaryDao,
    private val flashcardDao: FlashcardDao,
) {
    suspend fun getSummary(documentId: String, mode: SummaryMode, opts: SummarizeRequest): Summary {
        if (mode != SummaryMode.CUSTOM) {
            summaryDao.getSummary(documentId, mode.name)?.let {
                AppLog.d(TAG, "Summary local cache HIT (mode=$mode)")
                return it.toDomain()
            }
        }
        AppLog.i(TAG, "Requesting summary (mode=$mode) for doc=$documentId")
        val response = api.summarize(documentId, opts)
        AppLog.i(TAG, "Summary received (serverCached=${response.cached})")
        if (mode != SummaryMode.CUSTOM) {
            summaryDao.insertSummary(SummaryEntity(documentId, mode.name, response.summary))
        }
        return Summary(documentId, mode, response.summary, System.currentTimeMillis())
    }

    suspend fun getFlashcards(documentId: String, count: Int = 10): List<Flashcard> {
        flashcardDao.getFlashcardsForDocument(documentId).let { cached ->
            if (cached.isNotEmpty()) {
                AppLog.d(TAG, "Flashcards local cache HIT (${cached.size} cards)")
                return cached.map { it.toDomain() }
            }
        }
        AppLog.i(TAG, "Requesting $count flashcards for doc=$documentId")
        val response = api.generateFlashcards(documentId, FlashcardsRequest(count))
        AppLog.i(TAG, "Flashcards received: ${response.flashcards.size} (serverCached=${response.cached})")
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
        AppLog.i(TAG, "Requesting insights for doc=$documentId")
        val r = api.getInsights(documentId)
        AppLog.i(TAG, "Insights received: ${r.keyPoints.size} points, ${r.entities.size} entities (serverCached=${r.cached})")
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
        AppLog.i(TAG, "Requesting index for doc=$documentId")
        val r = api.getIndex(documentId)
        AppLog.i(TAG, "Index received: ${r.sections.size} sections (serverCached=${r.cached})")
        return DocumentIndex(
            documentId = documentId,
            sections   = r.sections.map { IndexSection(it.title, it.page, it.summary, it.level) },
        )
    }

    suspend fun explain(documentId: String, selectedText: String, mode: ExplainMode): ExplainResult {
        AppLog.i(TAG, "Requesting explain (mode=$mode, textLen=${selectedText.length}) for doc=$documentId")
        val r = api.explain(documentId, ExplainRequest(selectedText, mode.name.lowercase()))
        AppLog.i(TAG, "Explain received (${r.explanation.length} chars)")
        return ExplainResult(selectedText, mode, r.explanation)
    }
}
