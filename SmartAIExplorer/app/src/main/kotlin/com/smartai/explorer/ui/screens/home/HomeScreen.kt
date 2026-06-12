package com.smartai.explorer.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartai.explorer.ui.components.DocumentCard
import com.smartai.explorer.ui.components.PageRangeDialog
import com.smartai.explorer.ui.theme.Indigo50

@Composable
fun HomeScreen(
    onOpenViewer: (fileUri: String, fileName: String, startPage: Int, endPage: Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context      = LocalContext.current
    val documents    by viewModel.documents.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Take persistent read permission so the file can be re-opened later
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
        viewModel.onFilePicked(context, uri, name)
    }
    val openPicker = { launcher.launch(arrayOf("application/pdf")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        HomeTopBar(onOpenPdf = openPicker)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        if (documents.isEmpty()) {
            EmptyState(onOpenPdf = openPicker, modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
                Text(
                    text     = "Recent documents",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp),
                )
                LazyVerticalGrid(
                    columns               = GridCells.Adaptive(minSize = 260.dp),
                    modifier              = Modifier.fillMaxSize(),
                    contentPadding        = PaddingValues(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement   = Arrangement.spacedBy(16.dp),
                ) {
                    items(documents, key = { it.documentId }) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick  = {
                                // localFileUri is the local cache path after first open —
                                // parse as a file URI so ensureLocalCopy returns it as-is.
                                viewModel.onFilePicked(
                                    context,
                                    Uri.fromFile(java.io.File(doc.localFileUri)),
                                    doc.fileName,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    selectedFile?.let { state ->
        PageRangeDialog(
            fileName   = state.fileName,
            totalPages = state.totalPages,
            onConfirm  = { start, end ->
                viewModel.dismissDialog()
                onOpenViewer(state.localPath, state.fileName, start, end)
            },
            onDismiss  = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun HomeTopBar(onOpenPdf: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 32.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Brand mark
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onPrimary,
                modifier           = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text  = "SmartViewer",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "AI-powered PDF reading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick        = onOpenPdf,
            modifier       = Modifier.height(48.dp),
            shape          = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Open PDF", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyState(onOpenPdf: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Indigo50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Outlined.UploadFile,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Open your first document",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text      = "Pick a PDF to read, chat with it, summarise it,\nand turn it into flashcards.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick        = onOpenPdf,
                modifier       = Modifier.height(52.dp),
                shape          = MaterialTheme.shapes.small,
                contentPadding = PaddingValues(horizontal = 28.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open PDF", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
