package com.smartai.explorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MAX_PAGES = 10

@Composable
fun PageRangeDialog(
    fileName:   String,
    totalPages: Int,
    onConfirm:  (startPage: Int, endPage: Int) -> Unit,
    onDismiss:  () -> Unit,
) {
    var startPage by remember { mutableIntStateOf(1) }
    var endPage   by remember { mutableIntStateOf(minOf(MAX_PAGES, totalPages)) }

    // Keep end ≥ start and range ≤ MAX_PAGES
    fun clamp() {
        if (endPage < startPage)                  endPage   = startPage
        if (endPage - startPage + 1 > MAX_PAGES)  endPage   = startPage + MAX_PAGES - 1
        if (endPage > totalPages)                 endPage   = totalPages
        if (startPage < 1)                        startPage = 1
    }

    val pageCount = endPage - startPage + 1
    val atMax     = pageCount >= MAX_PAGES

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.large,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Choose pages", style = MaterialTheme.typography.titleLarge)
                Text(
                    text  = "$fileName · $totalPages page${if (totalPages != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    PageStepper(
                        label       = "From",
                        value       = startPage,
                        onDecrement = {
                            startPage = (startPage - 1).coerceAtLeast(1)
                            clamp()
                        },
                        onIncrement = {
                            startPage = (startPage + 1).coerceAtMost(endPage)
                            clamp()
                        },
                    )
                    Text(
                        "–",
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    PageStepper(
                        label       = "To",
                        value       = endPage,
                        onDecrement = {
                            endPage = (endPage - 1).coerceAtLeast(startPage)
                            clamp()
                        },
                        onIncrement = {
                            endPage = (endPage + 1).coerceAtMost(totalPages)
                            clamp()
                        },
                    )
                }

                // Page count pill
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val tint = if (atMax) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    val bg   = if (atMax) MaterialTheme.colorScheme.errorContainer
                               else MaterialTheme.colorScheme.primaryContainer
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text  = "$pageCount page${if (pageCount != 1) "s" else ""} selected" +
                                    if (atMax) " · max $MAX_PAGES" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(startPage, endPage) },
                enabled  = pageCount in 1..MAX_PAGES,
                modifier = Modifier.height(48.dp),
                shape    = MaterialTheme.shapes.small,
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PageStepper(
    label:       String,
    value:       Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label", modifier = Modifier.size(20.dp))
            }
            Text(
                text      = value.toString(),
                style     = MaterialTheme.typography.titleLarge,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier  = Modifier.widthIn(min = 48.dp),
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label", modifier = Modifier.size(20.dp))
            }
        }
    }
}
