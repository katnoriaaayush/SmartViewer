package com.smartai.explorer.ui.screens.home

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartai.explorer.data.repository.DocumentRepository
import com.smartai.explorer.domain.model.SmartDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SelectedFileState(
    val uri:        Uri,
    val fileName:   String,
    val totalPages: Int,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    val documents = documentRepository.documents.stateIn(
        scope             = viewModelScope,
        started           = SharingStarted.WhileSubscribed(5_000),
        initialValue      = emptyList<SmartDocument>(),
    )

    private val _selectedFile = MutableStateFlow<SelectedFileState?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    fun onFilePicked(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            val pages = runCatching { getPdfPageCount(context, uri) }.getOrDefault(1)
            _selectedFile.value = SelectedFileState(uri, fileName, pages)
        }
    }

    fun dismissDialog() { _selectedFile.value = null }

    private fun getPdfPageCount(context: Context, uri: Uri): Int =
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        } ?: 1
}
