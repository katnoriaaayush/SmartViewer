package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.DocumentInsights
import com.smartai.explorer.domain.model.UiState

@Composable
fun InsightsTab(
    state:    UiState<DocumentInsights>,
    onLoad:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Button(onClick = onLoad, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = state !is UiState.Loading) {
            Text(if (state is UiState.Loading) "Analysing…" else "Analyse Document")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is UiState.Success -> InsightsContent(insights = state.data)
            is UiState.Error   -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            else               -> {}
        }
    }
}

@Composable
private fun InsightsContent(insights: DocumentInsights) {
    var showEntities by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeader("Key Points") }
        items(insights.keyPoints) { point -> BulletItem(point) }

        item { SectionHeader("Topics") }
        items(insights.topics) { topic -> BulletItem(topic) }

        if (insights.actionItems.isNotEmpty()) {
            item { SectionHeader("Action Items") }
            items(insights.actionItems) { item -> BulletItem(item) }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Named Entities (${insights.entities.size})")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showEntities = !showEntities }) {
                    Text(if (showEntities) "Hide" else "Show")
                }
            }
        }
        if (showEntities) {
            items(insights.entities) { entity ->
                Row(modifier = Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(entity.type.name) })
                    Text(entity.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
}

@Composable
private fun BulletItem(text: String) {
    Text("• $text", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
}
