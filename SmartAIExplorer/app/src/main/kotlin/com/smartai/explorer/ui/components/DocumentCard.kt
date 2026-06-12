package com.smartai.explorer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartai.explorer.domain.model.SmartDocument
import com.smartai.explorer.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DocumentCard(
    document: SmartDocument,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick   = onClick,
        modifier  = modifier.fillMaxWidth(),
        shape     = MaterialTheme.shapes.medium,
        colors    = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border    = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // PDF icon tile
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(ErrorRed.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier           = Modifier.size(26.dp),
                    tint               = ErrorRed,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text     = document.fileName,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = "${formatFileSize(document.fileSize)}  ·  ${formatDate(document.uploadedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long) = when {
    bytes < 1024L        -> "$bytes B"
    bytes < 1024L * 1024 -> "${bytes / 1024} KB"
    else                 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}

private fun formatDate(epochMs: Long) =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epochMs))
