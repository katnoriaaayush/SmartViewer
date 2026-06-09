package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.DocumentIndex
import com.smartai.explorer.domain.model.IndexSection
import com.smartai.explorer.domain.model.UiState

@Composable
fun IndexTab(
    state:    UiState<DocumentIndex>,
    onLoad:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Button(onClick = onLoad, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = state !is UiState.Loading) {
            Text(if (state is UiState.Loading) "Building index…" else "Build Document Index")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Success -> IndexContent(index = state.data)
            is UiState.Error   -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            else               -> {}
        }
    }
}

@Composable
private fun IndexContent(index: DocumentIndex) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(index.sections) { section ->
            IndexSectionRow(section = section)
        }
    }
}

@Composable
private fun IndexSectionRow(section: IndexSection) {
    val indent = (section.level - 1) * 16
    Row(
        modifier          = Modifier.fillMaxWidth().padding(start = indent.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(section.title, style = if (section.level == 1) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyLarge)
            if (section.summary.isNotBlank()) {
                Text(section.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            text  = "p.${section.page}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}
