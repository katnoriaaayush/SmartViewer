package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.Summary
import com.smartai.explorer.domain.model.SummaryMode
import com.smartai.explorer.domain.model.UiState
import com.smartai.explorer.ui.components.MarkdownText

@Composable
fun SummaryTab(
    state:   UiState<Summary>,
    onLoad:  (SummaryMode, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMode  by remember { mutableStateOf(SummaryMode.QUICK) }
    var customPrompt  by remember { mutableStateOf("") }

    Column(
        modifier            = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Mode selector — segmented control
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SummaryMode.entries.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = mode == selectedMode,
                    onClick  = { selectedMode = mode },
                    shape    = SegmentedButtonDefaults.itemShape(index = i, count = SummaryMode.entries.size),
                    colors   = SegmentedButtonDefaults.colors(
                        activeContainerColor   = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor     = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                ) {
                    Text(
                        mode.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        if (selectedMode == SummaryMode.CUSTOM) {
            OutlinedTextField(
                value         = customPrompt,
                onValueChange = { customPrompt = it },
                label         = { Text("What should the summary focus on?") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = MaterialTheme.shapes.small,
                maxLines      = 3,
                textStyle     = MaterialTheme.typography.bodyMedium,
            )
        }

        Button(
            onClick  = { onLoad(selectedMode, customPrompt.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = MaterialTheme.shapes.small,
            enabled  = state !is UiState.Loading,
        ) {
            Text(if (state is UiState.Loading) "Generating…" else "Generate summary")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
            is UiState.Success -> OutlinedCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape    = MaterialTheme.shapes.medium,
                colors   = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                MarkdownText(
                    text     = state.data.content,
                    style    = MaterialTheme.typography.bodyLarge,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                )
            }
            is UiState.Error   -> Text(
                text  = state.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> {}
        }
    }
}
