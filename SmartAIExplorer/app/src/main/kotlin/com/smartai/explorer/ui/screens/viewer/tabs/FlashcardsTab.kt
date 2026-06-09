package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.Difficulty
import com.smartai.explorer.domain.model.Flashcard
import com.smartai.explorer.domain.model.UiState
import com.smartai.explorer.ui.theme.FlashcardAmber
import com.smartai.explorer.ui.theme.InsightsGreen
import com.smartai.explorer.ui.theme.SummaryPurple

@Composable
fun FlashcardsTab(
    state:    UiState<List<Flashcard>>,
    onLoad:   () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Button(
            onClick  = onLoad,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled  = state !is UiState.Loading,
        ) {
            Text(if (state is UiState.Loading) "Generating…" else "Generate Flashcards")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is UiState.Success -> FlashcardDeck(cards = state.data)
            is UiState.Error   -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            else -> {}
        }
    }
}

@Composable
private fun FlashcardDeck(cards: List<Flashcard>) {
    var index   by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    if (cards.isEmpty()) return

    val card    = cards[index]
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Counter
        Text(
            text  = "${index + 1} / ${cards.size}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Card — tap to flip
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
                .clickable { flipped = !flipped },
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(4.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize().graphicsLayer { if (rotation > 90f) rotationY = 180f },
                contentAlignment = Alignment.Center) {
                Text(
                    text      = if (rotation <= 90f) card.front else card.back,
                    style     = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(24.dp),
                )
            }
        }

        // Difficulty badge
        val (diffColor, diffLabel) = when (card.difficulty) {
            Difficulty.EASY   -> InsightsGreen to "Easy"
            Difficulty.MEDIUM -> FlashcardAmber to "Medium"
            Difficulty.HARD   -> SummaryPurple  to "Hard"
        }
        AssistChip(
            onClick  = {},
            label    = { Text(diffLabel) },
            colors   = AssistChipDefaults.assistChipColors(labelColor = diffColor),
        )

        // Prev / Next
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { index = (index - 1).coerceAtLeast(0); flipped = false }) { Text("← Prev") }
            TextButton(onClick = { index = (index + 1).coerceAtMost(cards.size - 1); flipped = false }) { Text("Next →") }
        }
    }
}
