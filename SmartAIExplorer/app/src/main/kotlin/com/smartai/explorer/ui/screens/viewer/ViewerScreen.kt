package com.smartai.explorer.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartai.explorer.domain.model.AiFeature
import com.smartai.explorer.domain.model.UiState

@Composable
fun ViewerScreen(
    fileUri:   String,
    fileName:  String,
    startPage: Int,
    endPage:   Int,
    onBack:    () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(fileUri) { viewModel.uploadDocument(fileUri) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ViewerTopBar(
            fileName  = fileName,
            startPage = startPage,
            endPage   = endPage,
            zoomPct   = (state.zoomScale * 100).toInt(),
            onBack    = onBack,
            onZoomIn  = viewModel::zoomIn,
            onZoomOut = viewModel::zoomOut,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // ── Two-panel body ────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxSize()) {

            // PDF panel — 62%
            Box(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
            ) {
                PdfPanel(
                    fileUri        = fileUri,
                    startPage      = startPage,
                    endPage        = endPage,
                    zoomScale      = state.zoomScale,
                    scrollTarget   = state.scrollTarget,
                    onTextSelected = viewModel::onTextSelected,
                    modifier       = Modifier.fillMaxSize(),
                )

                // Upload loading overlay — shown while document is being sent to server
                if (state.uploadState is UiState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape           = MaterialTheme.shapes.medium,
                            color           = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                        ) {
                            Row(
                                modifier              = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier    = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                )
                                Text(
                                    text  = "Preparing document…",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // Upload error overlay
                if (state.uploadState is UiState.Error) {
                    val errorMsg = (state.uploadState as UiState.Error).message
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape           = MaterialTheme.shapes.medium,
                            color           = MaterialTheme.colorScheme.surface,
                            shadowElevation = 8.dp,
                            modifier        = Modifier.widthIn(max = 420.dp),
                        ) {
                            Column(
                                modifier            = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text  = "Couldn't upload document",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text      = errorMsg,
                                    style     = MaterialTheme.typography.bodyMedium,
                                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick  = { viewModel.uploadDocument(fileUri) },
                                    modifier = Modifier.height(48.dp),
                                    shape    = MaterialTheme.shapes.small,
                                ) {
                                    Text("Try again")
                                }
                            }
                        }
                    }
                }

                // Floating text-selection toolbar
                if (state.selectedText != null) {
                    TextSelectionToolbar(
                        onSummarize  = {
                            viewModel.clearSelectedText()
                            viewModel.loadSummary()
                        },
                        onAsk        = { text ->
                            viewModel.clearSelectedText()
                            viewModel.setActiveFeature(AiFeature.CHAT)
                            viewModel.sendChatMessage(text)
                        },
                        onExplain    = { viewModel.explainSelectedText() },
                        selectedText = state.selectedText!!,
                        modifier     = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                    )
                }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outline)

            // AI panel — 38%
            AiPanel(
                state         = state,
                viewModel     = viewModel,
                onRetryUpload = { viewModel.uploadDocument(fileUri) },
                modifier      = Modifier.weight(0.38f).fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun ViewerTopBar(
    fileName:  String,
    startPage: Int,
    endPage:   Int,
    zoomPct:   Int,
    onBack:    () -> Unit,
    onZoomIn:  () -> Unit,
    onZoomOut: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = fileName,
                style    = MaterialTheme.typography.titleMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text  = "Pages $startPage–$endPage",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Zoom pill — grouped −/value/+ control
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onZoomOut, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Zoom out",
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text      = "$zoomPct%",
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier  = Modifier.widthIn(min = 48.dp),
            )
            IconButton(onClick = onZoomIn, modifier = Modifier.size(44.dp)) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Zoom in",
                    tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
    }
}
