package com.smartai.explorer.ui.screens.home

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartai.explorer.data.repository.DocumentRepository
import com.smartai.explorer.domain.model.SmartDocument
import com.smartai.explorer.util.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "HomeVM"

data class SelectedFileState(
    val localPath:  String,   // absolute path inside filesDir/pdfs — no content URI needed
    val fileName:   String,
    val totalPages: Int,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
) : ViewModel() {

    val documents = documentRepository.documents.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<SmartDocument>(),
    )

    private val _selectedFile = MutableStateFlow<SelectedFileState?>(null)
    val selectedFile = _selectedFile.asStateFlow()

    /**
     * Called when the user picks a PDF via the file picker OR taps a DocumentCard.
     *
     * [uri] may be a content:// URI (fresh from the picker) or a file:// / plain-path
     * URI (stored in the database after a previous successful open).
     *
     * For content:// URIs we copy the bytes to app-private storage immediately while
     * the temporary ACTION_OPEN_DOCUMENT permission is still active.  All subsequent
     * reads (upload to server, PDF.js injection) use the stable local path so they
     * are not affected by permission expiry.
     */
    fun onFilePicked(context: Context, uri: Uri, fileName: String) {
        AppLog.i(TAG, "File picked: $fileName ($uri)")
        viewModelScope.launch {
            runCatching {
                val localPath = withContext(Dispatchers.IO) { ensureLocalCopy(context, uri, fileName) }
                val pages     = withContext(Dispatchers.IO) { getPageCount(localPath) }
                AppLog.i(TAG, "Local copy ready: $localPath ($pages pages)")
                SelectedFileState(localPath, fileName, pages)
            }.onSuccess { state ->
                _selectedFile.value = state
            }.onFailure { e ->
                AppLog.e(TAG, "Failed to prepare picked file", e)
            }
        }
    }

    fun dismissDialog() { _selectedFile.value = null }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun ensureLocalCopy(context: Context, uri: Uri, fileName: String): String {
        // If the stored value is already an accessible local file, return it as-is.
        val path = uri.path
        if ((uri.scheme == null || uri.scheme == "file") && path != null && File(path).exists()) {
            return path
        }

        val pdfDir   = File(context.filesDir, "pdfs").also { it.mkdirs() }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dest     = File(pdfDir, "doc_${Integer.toHexString(uri.toString().hashCode())}_$safeName")

        if (dest.exists()) return dest.absolutePath   // already cached

        context.contentResolver.openInputStream(uri)!!.use { src ->
            dest.outputStream().use { dst -> src.copyTo(dst) }
        }
        return dest.absolutePath
    }

    private fun getPageCount(localPath: String): Int =
        ParcelFileDescriptor.open(File(localPath), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { it.pageCount }
        }
}
