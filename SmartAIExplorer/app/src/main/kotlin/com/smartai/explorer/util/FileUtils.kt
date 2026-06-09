package com.smartai.explorer.util

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Build a multipart form-data part directly from a local file.
 * No ContentResolver or URI permissions needed.
 */
fun File.toMultipartPart(fieldName: String = "file"): MultipartBody.Part {
    val body = readBytes().toRequestBody("application/pdf".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(fieldName, name, body)
}
