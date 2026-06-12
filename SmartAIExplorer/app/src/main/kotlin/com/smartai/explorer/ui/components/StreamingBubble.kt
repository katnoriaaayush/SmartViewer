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
        shape    = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.widthIn(max = 360.dp),
    ) {
        // Cursor blink is visual-only — just append the block character
        Text(
            text     = "$text▋",
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
