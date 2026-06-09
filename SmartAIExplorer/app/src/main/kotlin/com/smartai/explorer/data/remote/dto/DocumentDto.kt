package com.smartai.explorer.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UploadDocumentResponse(
    @SerializedName("documentId") val documentId: String,
    @SerializedName("sessionId")  val sessionId:  String,
    @SerializedName("fileName")   val fileName:   String,
    @SerializedName("mimeType")   val mimeType:   String,
    @SerializedName("fileSize")   val fileSize:   Long,
)
