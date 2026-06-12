package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.DocumentInsights
import com.smartai.explorer.domain.model.UiState
import com.smartai.explorer.ui.theme.*

@Composable
fun InsightsTab(
    state:    UiState<DocumentInsights>,
    onLoad:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick  = onLoad,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = MaterialTheme.shapes.small,
            enabled  = state !is UiState.Loading,
        ) {
            Text(if (state is UiState.Loading) "Analysing…" else "Analyse document")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
            is UiState.Success -> InsightsContent(insights = state.data)
            is UiState.Error   -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            else               -> {}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InsightsContent(insights: DocumentInsights) {
    var showEntities by remember { mutableStateOf(false) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        item { SectionHeader("Key points", InsightsGreen) }
        items(insights.keyPoints) { point -> BulletItem(point, InsightsGreen) }

        item { SectionHeader("Topics", SummaryPurple) }
        item {
            // Topics read better as a wrapping chip set than a list
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                insights.topics.forEach { topic ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SummaryPurpleSoft)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(topic, style = MaterialTheme.typography.labelMedium, color = SummaryPurple)
                    }
                }
            }
        }

        if (insights.actionItems.isNotEmpty()) {
            item { SectionHeader("Action items", ChatBlue) }
            items(insights.actionItems) { item -> BulletItem(item, ChatBlue) }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                SectionHeader("Entities (${insights.entities.size})", IndexSlate)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showEntities = !showEntities }) {
                    Text(if (showEntities) "Hide" else "Show", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (showEntities) {
            items(insights.entities) { entity ->
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier              = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndexSlateSoft)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            entity.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = IndexSlate,
                        )
                    }
                    Text(entity.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BulletItem(text: String, accent: Color) {
    Row(modifier = Modifier.padding(start = 16.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium, color = accent)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
