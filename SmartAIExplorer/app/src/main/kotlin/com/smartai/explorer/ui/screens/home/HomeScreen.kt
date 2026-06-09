package com.smartai.explorer.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartai.explorer.ui.components.DocumentCard
import com.smartai.explorer.ui.components.PageRangeDialog

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("SmartViewer", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch(arrayOf("application/pdf")) },
                icon    = { Icon(Icons.Default.Add, contentDescription = "Open PDF") },
                text    = { Text("Open PDF") },
                modifier = Modifier.height(64.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (documents.isEmpty()) {
            EmptyState(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            LazyVerticalGrid(
                columns               = GridCells.Adaptive(minSize = 220.dp),
                modifier              = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                contentPadding        = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement   = Arrangement.spacedBy(16.dp),
            ) {
                items(documents, key = { it.documentId }) { doc ->
                    DocumentCard(
                        document = doc,
                        onClick  = {
                            viewModel.onFilePicked(context, Uri.parse(doc.localFileUri), doc.fileName)
                        },
                    )
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
                onOpenViewer(state.uri.toString(), state.fileName, start, end)
            },
            onDismiss  = viewModel::dismissDialog,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No documents yet", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tap + Open PDF to get started", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
