package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.DocumentIndex
import com.smartai.explorer.domain.model.IndexSection
import com.smartai.explorer.domain.model.UiState
import com.smartai.explorer.ui.theme.IndexSlate
import com.smartai.explorer.ui.theme.IndexSlateSoft

@Composable
fun IndexTab(
    state:          UiState<DocumentIndex>,
    onLoad:         () -> Unit,
    onScrollToPage: (Int) -> Unit,
    modifier:       Modifier = Modifier,
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
            Text(if (state is UiState.Loading) "Building index…" else "Build document index")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
            is UiState.Success -> IndexContent(index = state.data, onScrollToPage = onScrollToPage)
            is UiState.Error   -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            else               -> {}
        }
    }
}

@Composable
private fun IndexContent(index: DocumentIndex, onScrollToPage: (Int) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        items(index.sections) { section ->
            IndexSectionRow(section = section, onScrollToPage = onScrollToPage)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun IndexSectionRow(section: IndexSection, onScrollToPage: (Int) -> Unit) {
    val indent = (section.level - 1) * 16
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onScrollToPage(section.page) }
            .padding(start = indent.dp + 4.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = section.title,
                style = if (section.level == 1) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (section.summary.isNotBlank()) {
                Text(
                    text  = section.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Page number acts as a visual tap affordance
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(IndexSlateSoft)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text  = "p. ${section.page}",
                style = MaterialTheme.typography.labelMedium,
                color = IndexSlate,
            )
        }
    }
}
