package com.smartai.explorer.util

import android.util.Log
import com.smartai.explorer.BuildConfig

/**
 * Thin wrapper around android.util.Log for consistent, filterable logging.
 *
 * All tags are prefixed with "SmartAI/" so the whole app can be filtered with
 * `adb logcat -s SmartAI/*`. Verbose and debug logs are compiled out in release
 * builds via the BuildConfig.DEBUG guard; info/warn/error always emit.
 */
object AppLog {
    private const val PREFIX = "SmartAI"

    private fun tag(tag: String) = "$PREFIX/$tag"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag(tag), message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag(tag), message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag(tag), message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag(tag), message, throwable)
    }
}
