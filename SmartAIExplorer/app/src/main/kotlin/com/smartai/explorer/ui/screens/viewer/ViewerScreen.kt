package com.smartai.explorer.ui.screens.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartai.explorer.domain.model.UiState

@OptIn(ExperimentalMaterial3Api::class)
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
        // ── Top bar ──────────────────────────────────────────────────────────
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            title = {
                Column {
                    Text(
                        text     = fileName,
                        style    = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text  = "Pages $startPage – $endPage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            actions = {
                IconButton(onClick = viewModel::zoomOut, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                }
                Text(
                    text     = "${(state.zoomScale * 100).toInt()}%",
                    style    = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = viewModel::zoomIn, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                }
                Spacer(Modifier.width(16.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        )

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
                    Surface(
                        modifier       = Modifier.fillMaxSize(),
                        color          = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text  = "Uploading to AI server…",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // Upload error overlay
                if (state.uploadState is UiState.Error) {
                    val errorMsg = (state.uploadState as UiState.Error).message
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color    = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Card(
                                modifier = Modifier.padding(32.dp),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                ),
                            ) {
                                Column(
                                    modifier            = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                ) {
                                    Text(
                                        text  = "Upload failed",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text  = errorMsg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Button(
                                        onClick  = { viewModel.uploadDocument(fileUri) },
                                        modifier = Modifier.height(56.dp),
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                        ),
                                    ) {
                                        Text("Retry Upload")
                                    }
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
                            viewModel.setActiveFeature(com.smartai.explorer.domain.model.AiFeature.CHAT)
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
