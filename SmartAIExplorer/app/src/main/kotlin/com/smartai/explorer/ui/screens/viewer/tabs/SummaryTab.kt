package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.Summary
import com.smartai.explorer.domain.model.SummaryMode
import com.smartai.explorer.domain.model.UiState

@Composable
fun SummaryTab(
    state:   UiState<Summary>,
    onLoad:  (SummaryMode, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedMode  by remember { mutableStateOf(SummaryMode.QUICK) }
    var customPrompt  by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // Mode selector chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryMode.entries.forEach { mode ->
                FilterChip(
                    selected = mode == selectedMode,
                    onClick  = { selectedMode = mode },
                    label    = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.height(48.dp),
                    shape    = RoundedCornerShape(50),
                )
            }
        }

        if (selectedMode == SummaryMode.CUSTOM) {
            OutlinedTextField(
                value         = customPrompt,
                onValueChange = { customPrompt = it },
                label         = { Text("Custom prompt") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                maxLines      = 3,
            )
        }

        Button(
            onClick  = { onLoad(selectedMode, customPrompt.ifBlank { null }) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled  = state !is UiState.Loading,
        ) {
            Text(if (state is UiState.Loading) "Generating…" else "Generate Summary")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Success -> Text(
                text     = state.data.content,
                style    = MaterialTheme.typography.bodyLarge,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
            is UiState.Error   -> Text(
                text  = "Error: ${state.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> {}
        }
    }
}
