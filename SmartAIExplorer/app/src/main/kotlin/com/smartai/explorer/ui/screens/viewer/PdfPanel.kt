package com.smartai.explorer.ui.screens.viewer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * PDF viewer panel.
 *
 * Phase 2: Shows a placeholder.
 * Phase 3: Replaced with WebView + PDF.js via WebViewAssetLoader.
 *          PDF bytes are read from [fileUri], base64-encoded, and injected
 *          via evaluateJavascript("window.loadPDF('...')") after onPageFinished.
 *          Text selection flows: selectionchange → AndroidBridge.onTextSelected(text)
 *          → [onTextSelected].
 */
@Composable
fun PdfPanel(
    fileUri:        String,
    startPage:      Int,
    endPage:        Int,
    onTextSelected: (String) -> Unit,
    modifier:       Modifier = Modifier,
) {
    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text  = "PDF viewer coming in Phase 3",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = "Pages $startPage–$endPage of ${fileUri.substringAfterLast('/')}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
