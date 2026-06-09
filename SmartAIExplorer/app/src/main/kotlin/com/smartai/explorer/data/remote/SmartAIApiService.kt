package com.smartai.explorer.data.remote

import com.smartai.explorer.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.*

interface SmartAIApiService {

    @Multipart
    @POST("api/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
    ): UploadDocumentResponse

    @GET("api/documents")
    suspend fun listDocuments(): List<UploadDocumentResponse>

    // @Streaming + non-suspend so OkHttp doesn't buffer the SSE body
    @POST("api/documents/{id}/chat")
    @Streaming
    fun chat(
        @Path("id") documentId: String,
        @Body request: ChatRequest,
    ): retrofit2.Call<ResponseBody>

    @POST("api/documents/{id}/summarize")
    suspend fun summarize(
        @Path("id") documentId: String,
        @Body request: SummarizeRequest,
    ): SummarizeResponse

    @POST("api/documents/{id}/flashcards")
    suspend fun generateFlashcards(
        @Path("id") documentId: String,
        @Body request: FlashcardsRequest,
    ): FlashcardsResponse

    @GET("api/documents/{id}/insights")
    suspend fun getInsights(@Path("id") documentId: String): InsightsResponse

    @GET("api/documents/{id}/index")
    suspend fun getIndex(@Path("id") documentId: String): IndexResponse

    @POST("api/documents/{id}/explain")
    suspend fun explain(
        @Path("id") documentId: String,
        @Body request: ExplainRequest,
    ): ExplainResponse

    @GET("api/sessions/{sessionId}/history")
    suspend fun getChatHistory(@Path("sessionId") sessionId: String): ChatHistoryResponse

    @DELETE("api/sessions/{sessionId}/history")
    suspend fun clearChatHistory(@Path("sessionId") sessionId: String)
}
