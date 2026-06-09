package com.smartai.explorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StreamingBubble(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape          = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
        color          = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp,
        modifier       = modifier.widthIn(max = 360.dp),
    ) {
        // Cursor blink is visual-only — just append the block character
        Text(
            text     = "$text▋",
            style    = MaterialTheme.typography.bodyLarge,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp),
        )
    }
}
