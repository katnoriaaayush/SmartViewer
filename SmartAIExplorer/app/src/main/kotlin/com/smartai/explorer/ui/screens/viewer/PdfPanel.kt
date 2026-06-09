package com.smartai.explorer.ui.screens.viewer

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader

private const val TAG = "PdfPanel"

/**
 * Renders a page range from a PDF file using PDF.js 6 inside a WebView.
 *
 * Assets are served via WebViewAssetLoader at:
 *   https://appassets.androidplatform.net/assets/pdfjs/viewer.html
 *
 * PDF bytes are read from [fileUri] on a background thread, base64-encoded
 * (NO_WRAP, so no newlines), then injected via evaluateJavascript after
 * onPageFinished. Text-selection events flow back through AndroidBridge.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PdfPanel(
    fileUri:        String,
    startPage:      Int,
    endPage:        Int,
    zoomScale:      Float,
    scrollTarget:   Pair<Int, Long>?,
    onTextSelected: (String) -> Unit,
    modifier:       Modifier = Modifier,
) {
    // rememberUpdatedState so the lambdas captured by the WebViewClient always
    // see the latest values without needing to recreate the WebView.
    val onTextSelectedState = rememberUpdatedState(onTextSelected)
    val currentZoom         = rememberUpdatedState(zoomScale)

    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val pdfLoaded  = remember { mutableStateOf(false) }

    // Forward zoom changes to viewer.html after the PDF is loaded
    LaunchedEffect(zoomScale) {
        if (pdfLoaded.value) {
            webViewRef.value?.evaluateJavascript("window.setZoom($zoomScale)", null)
        }
    }

    // Forward scroll-to-page requests; nonce in the Pair ensures the same page
    // number can be re-requested after the user has scrolled away manually.
    LaunchedEffect(scrollTarget) {
        if (pdfLoaded.value && scrollTarget != null) {
            webViewRef.value?.evaluateJavascript("window.scrollToPage(${scrollTarget.first})", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose { webViewRef.value?.destroy() }
    }

    AndroidView(
        modifier = modifier,
        factory  = { context ->
            val assetLoader = WebViewAssetLoader.Builder()
                .setDomain("appassets.androidplatform.net")
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled  = true
                    allowFileAccess    = false   // assets served via AssetLoader, not file://
                    allowContentAccess = false
                    domStorageEnabled  = true
                }

                addJavascriptInterface(
                    object : Any() {
                        @JavascriptInterface
                        fun onTextSelected(text: String) {
                            // JS interface calls arrive on a background thread
                            Handler(Looper.getMainLooper()).post {
                                onTextSelectedState.value(text)
                            }
                        }

                        @JavascriptInterface
                        fun log(msg: String) {
                            Log.d(TAG, msg)
                        }
                    },
                    "AndroidBridge",
                )

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view:    WebView,
                        request: WebResourceRequest,
                    ) = assetLoader.shouldInterceptRequest(request.url)

                    override fun onPageFinished(view: WebView, url: String) {
                        // Read PDF bytes off the main thread; base64 strings can be large
                        Thread {
                            try {
                                val bytes = context.contentResolver
                                    .openInputStream(Uri.parse(fileUri))
                                    ?.use { it.readBytes() }
                                    ?: run {
                                        Log.e(TAG, "Cannot open PDF URI: $fileUri")
                                        return@Thread
                                    }

                                // Base64 alphabet has no single-quote chars — safe to embed
                                val b64  = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                val zoom = currentZoom.value

                                view.post {
                                    view.evaluateJavascript(
                                        "window.loadPDF('$b64',$startPage,$endPage)",
                                        null,
                                    )
                                    // If the user already changed zoom before loading finished,
                                    // setZoom updates the scale variable so renderRange picks it up.
                                    if (zoom != 1.5f) {
                                        view.evaluateJavascript("window.setZoom($zoom)", null)
                                    }
                                    pdfLoaded.value = true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to inject PDF", e)
                            }
                        }.start()
                    }
                }

                loadUrl("https://appassets.androidplatform.net/assets/pdfjs/viewer.html")
            }.also { webViewRef.value = it }
        },
    )
}
