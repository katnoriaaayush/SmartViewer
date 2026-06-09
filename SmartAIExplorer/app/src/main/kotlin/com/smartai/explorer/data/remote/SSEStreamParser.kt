package com.smartai.explorer.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody

object SSEStreamParser {
    private const val DATA_PREFIX   = "data: "
    private const val DONE_SIGNAL   = "[DONE]"
    private const val ERROR_PREFIX  = "[ERROR]"

    fun parse(body: ResponseBody): Flow<String> = flow {
        body.use { rb ->
            rb.source().use { src ->
                while (!src.exhausted()) {
                    val line = src.readUtf8Line() ?: break
                    if (!line.startsWith(DATA_PREFIX)) continue

                    val data = line.removePrefix(DATA_PREFIX)
                    when {
                        data == DONE_SIGNAL             -> return@flow
                        data.startsWith(ERROR_PREFIX)   -> throw Exception(data.removePrefix("$ERROR_PREFIX "))
                        data.isNotBlank()               -> emit(data)
                    }
                }
            }
        }
    }
}
