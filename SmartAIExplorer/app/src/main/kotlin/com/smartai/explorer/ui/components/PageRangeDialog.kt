package com.smartai.explorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        if (endPage < startPage)                      endPage   = startPage
        if (endPage - startPage + 1 > MAX_PAGES)      endPage   = startPage + MAX_PAGES - 1
        if (endPage > totalPages)                      endPage   = totalPages
        if (startPage < 1)                             startPage = 1
    }

    val pageCount = endPage - startPage + 1
    val atMax     = pageCount >= MAX_PAGES

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Pages to View", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(
                    text  = "$fileName  ·  $totalPages page${if (totalPages != 1) "s" else ""} total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    PageStepper(
                        label      = "Start",
                        value      = startPage,
                        onDecrement = {
                            startPage = (startPage - 1).coerceAtLeast(1)
                            clamp()
                        },
                        onIncrement = {
                            startPage = (startPage + 1).coerceAtMost(endPage)
                            clamp()
                        },
                    )

                    Text("to", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))

                    PageStepper(
                        label      = "End",
                        value      = endPage,
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

                // Page count badge
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    val color = if (atMax) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                    Text(
                        text  = "$pageCount page${if (pageCount != 1) "s" else ""} selected" +
                                if (atMax) "  (max $MAX_PAGES)" else "",
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { onConfirm(startPage, endPage) },
                enabled  = pageCount in 1..MAX_PAGES,
                modifier = Modifier.height(56.dp),
            ) {
                Text("Open Pages")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(56.dp)) {
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
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onDecrement, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease $label")
            }
            Text(
                text      = value.toString(),
                style     = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier  = Modifier.widthIn(min = 56.dp),
            )
            IconButton(onClick = onIncrement, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Increase $label")
            }
        }
    }
}
