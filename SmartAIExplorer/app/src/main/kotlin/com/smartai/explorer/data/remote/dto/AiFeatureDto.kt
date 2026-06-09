package com.smartai.explorer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SummarizeRequest(
    @SerializedName("mode")           val mode:           String,
    @SerializedName("customPrompt")   val customPrompt:   String? = null,
    @SerializedName("targetAudience") val targetAudience: String? = null,
    @SerializedName("wordLimit")      val wordLimit:      Int?    = null,
)

data class SummarizeResponse(
    @SerializedName("summary") val summary: String,
    @SerializedName("mode")    val mode:    String,
    @SerializedName("cached")  val cached:  Boolean,
)

data class FlashcardsRequest(@SerializedName("count") val count: Int = 10)

data class FlashcardDto(
    @SerializedName("id")         val id:         Long?   = null,
    @SerializedName("front")      val front:      String,
    @SerializedName("back")       val back:       String,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("page")       val page:       Int?,
)

data class FlashcardsResponse(
    @SerializedName("flashcards") val flashcards: List<FlashcardDto>,
    @SerializedName("cached")     val cached:     Boolean,
)

data class InsightsResponse(
    @SerializedName("keyPoints")   val keyPoints:   List<String>,
    @SerializedName("entities")    val entities:    List<EntityDto>,
    @SerializedName("actionItems") val actionItems: List<String>,
    @SerializedName("topics")      val topics:      List<String>,
    @SerializedName("cached")      val cached:      Boolean,
)

data class EntityDto(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String,
)

data class IndexResponse(
    @SerializedName("sections") val sections: List<IndexSectionDto>,
    @SerializedName("cached")   val cached:   Boolean,
)

data class IndexSectionDto(
    @SerializedName("title")   val title:   String,
    @SerializedName("page")    val page:    Int,
    @SerializedName("summary") val summary: String,
    @SerializedName("level")   val level:   Int,
)

data class ExplainRequest(
    @SerializedName("selectedText") val selectedText: String,
    @SerializedName("mode")         val mode:         String,
)

data class ExplainResponse(
    @SerializedName("explanation")  val explanation:  String,
    @SerializedName("selectedText") val selectedText: String,
    @SerializedName("mode")         val mode:         String,
)
