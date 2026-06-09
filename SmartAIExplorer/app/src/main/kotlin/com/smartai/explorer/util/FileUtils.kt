package com.smartai.explorer.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

fun Uri.toMultipartPart(context: Context, fieldName: String = "file"): MultipartBody.Part? {
    val mimeType = context.contentResolver.getType(this) ?: "application/pdf"
    val bytes    = context.contentResolver.openInputStream(this)?.use { it.readBytes() } ?: return null
    val body     = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
    val fileName = lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
    return MultipartBody.Part.createFormData(fieldName, fileName, body)
}
