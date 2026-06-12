package com.smartai.explorer.ui.screens.viewer.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.Difficulty
import com.smartai.explorer.domain.model.Flashcard
import com.smartai.explorer.domain.model.UiState
import com.smartai.explorer.ui.theme.*

@Composable
fun FlashcardsTab(
    state:    UiState<List<Flashcard>>,
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
            Text(if (state is UiState.Loading) "Generating…" else "Generate flashcards")
        }

        when (state) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
            is UiState.Success -> FlashcardDeck(cards = state.data)
            is UiState.Error   -> Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            else -> {}
        }
    }
}

@Composable
private fun FlashcardDeck(cards: List<Flashcard>) {
    var index   by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    if (cards.isEmpty()) return

    val card     = cards[index]
    val rotation by animateFloatAsState(if (flipped) 180f else 0f, label = "flip")
    val showingBack = rotation > 90f

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Progress
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text  = "Card ${index + 1} of ${cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            DifficultyBadge(card.difficulty)
        }
        LinearProgressIndicator(
            progress   = { (index + 1f) / cards.size },
            modifier   = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color      = FlashcardAmber,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        // Card — tap to flip
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
                .clickable { flipped = !flipped },
            shape  = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (showingBack) FlashcardAmberSoft else MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, if (showingBack) FlashcardAmber.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer { if (rotation > 90f) rotationY = 180f },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier            = Modifier.padding(24.dp),
                ) {
                    Text(
                        text  = if (showingBack) "ANSWER" else "QUESTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showingBack) FlashcardAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text      = if (showingBack) card.back else card.front,
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            text      = "Tap the card to flip",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth(),
        )

        // Prev / Next
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = { index = (index - 1).coerceAtLeast(0); flipped = false },
                enabled = index > 0,
                shape   = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Previous")
            }
            OutlinedButton(
                onClick = { index = (index + 1).coerceAtMost(cards.size - 1); flipped = false },
                enabled = index < cards.size - 1,
                shape   = MaterialTheme.shapes.small,
            ) {
                Text("Next")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val (color, soft, label) = when (difficulty) {
        Difficulty.EASY   -> Triple(InsightsGreen,  InsightsGreenSoft,  "Easy")
        Difficulty.MEDIUM -> Triple(FlashcardAmber, FlashcardAmberSoft, "Medium")
        Difficulty.HARD   -> Triple(ErrorRed,       ErrorContainer,     "Hard")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(soft)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
