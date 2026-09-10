package eu.nctools.app.ui.tools

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.nctools.app.core.tools.ToolCatalog

/**
 * Per-tool screen. After input, "Convert" triggers one full-screen 15s ad;
 * on dismissal the on-device conversion runs and the output file is ready to
 * share. Uses the same file picker API across tools.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    slug: String,
    viewModel: ToolsViewModel,
    authViewModel: eu.nctools.app.ui.auth.AuthViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tool = remember(slug) { ToolCatalog.bySlug(slug) }
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    var pickedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var cameraBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) pickedUris = uris
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pickedUris = listOf(uri)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) pickedUris = uris
    }

    remember(slug) { viewModel.configure(slug) }

    fun startConversion() {
        val eng = viewModel.toolEngine()
        when (slug) {
            "paste-to-pdf" -> viewModel.convert(activity!!) {
                eng.pasteToPdf(inputText.ifBlank { " " })
            }
            "pdf-to-word" -> viewModel.convert(activity!!) {
                eng.pdfToWord(pickedUris.first())
            }
            "pdf-to-excel" -> viewModel.convert(activity!!) {
                eng.pdfToExcel(pickedUris.first())
            }
            "pdf-to-ocr" -> viewModel.convert(activity!!) {
                eng.pdfToOcr(pickedUris.first())
            }
            "ocr-to-text" -> viewModel.convert(activity!!) {
                eng.ocrToText(pickedUris.first())
            }
            "photo-scanner" -> viewModel.convert(activity!!) {
                val bmp = cameraBitmap
                    ?: throw IllegalStateException("Capture a photo first.")
                eng.photoScanner(bmp)
            }
            "images-to-pdf" -> viewModel.convert(activity!!) {
                eng.imagesToPdf(pickedUris.map { uri ->
                    val bmp = context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                        ?: android.graphics.Bitmap.createBitmap(1,1,android.graphics.Bitmap.Config.ARGB_8888)
                    bmp
                })
            }
            "merge-pdf" -> viewModel.convert(activity!!) {
                if (pickedUris.size < 2) {
                    throw IllegalStateException("Pick at least two PDFs to merge.")
                }
                eng.mergePdf(pickedUris)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tool?.title ?: slug) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(tool?.subtitle ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            when (slug) {
                "paste-to-pdf" -> {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Paste or type your content") },
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        keyboardOptions = KeyboardOptions.Default,
                    )
                }
                "pdf-to-word", "pdf-to-excel", "pdf-to-ocr", "ocr-to-text" -> {
                    OutlinedButton(onClick = { singlePicker.launch(arrayOf("application/pdf", "image/*")) }) {
                        Text(if (pickedUris.isEmpty()) "Pick a file" else "Picked: ${pickedUris.first()?.lastPathSegment}")
                    }
                }
                "photo-scanner" -> {
                    OutlinedButton(onClick = { /* camera intent — wire to TakePicture */ }) {
                        Text("Take a photo")
                    }
                    if (cameraBitmap != null) Text("Photo captured", color = MaterialTheme.colorScheme.primary)
                }
                "images-to-pdf" -> {
                    OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                        Text(if (pickedUris.isEmpty()) "Pick images" else "Picked ${pickedUris.size} image(s)")
                    }
                }
                "merge-pdf" -> {
                    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                        Text(if (pickedUris.size < 2) "Pick PDF files (≥ 2)" else "Picked ${pickedUris.size} PDFs (tap to add more)")
                    }
                }
            }

            Button(
                onClick = {
                    if (slug == "ocr-to-text" && pickedUris.isEmpty()) return@Button
                    startConversion()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.running,
            ) {
                if (state.running) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Convert")
            }
            Text(
                "One 15-second ad unlocks this conversion.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            if (state.error != null) Text(state.error!!, color = MaterialTheme.colorScheme.error)
            if (state.resultPath != null) {
                Text("Done — file ready to share.", color = MaterialTheme.colorScheme.primary)
                OutlinedButton(
                    onClick = { viewModel.clearResult() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Start another") }
            }
        }
    }
}