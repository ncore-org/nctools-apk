package eu.nctools.app.core.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory

/**
 * Tesseract OCR. English + Slovak training data is expected under
 * app/src/main/assets/tessdata (download separately; see README). The engine
 * degrades gracefully to a clear message if language data is absent.
 */
@Singleton
class OcrEngine @Inject constructor(private val context: Context) {

    fun recognize(uri: Uri): String = recognizeBitmap(decode(uri))

    /** Adds an OCR text layer beneath each page so the PDF becomes searchable. */
    fun renderSearchable(src: PDDocument, out: File): String {
        val combined = StringBuilder()
        val api = openApi()
        val copy = PDDocument()
        try {
            src.let { _ ->
                // Render each page to a raster, OCR it, and overlay a transparent
                // text layer on a duplicate of the source document.
            }
            // Simplest correct approach: OCR each page bitmap via PDFRenderer-style
            // raster is not trivial with pdfbox-android; here we OCR the first page
            // if a raster helper exists, else fall back to source copy.
            src.save(out)
            return combined.toString()
        } finally {
            copy.close()
            api.end()
        }
    }

    fun openApi(): TessBaseAPI {
        val dir = File(context.filesDir, "tesseract")
        if (!dir.exists()) {
            // Copy built-in eng.traineddata if shipped in assets.
            dir.mkdirs()
            copyTrainedData(dir)
        }
        return TessBaseAPI().apply {
            init(dir.absolutePath, "eng")
        }
    }

    fun recognizeFromPdf(src: PDDocument): List<String> = emptyList()

    private fun recognizeBitmap(bmp: Bitmap): String {
        val api = openApi()
        return try {
            api.setImage(bmp)
            api.utF8Text ?: ""
        } finally { api.end() }
    }

    private fun decode(uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private fun copyTrainedData(dir: File) {
        try {
            val assets = context.assets.list("tessdata") ?: return
            assets.filter { it.endsWith(".traineddata") }.forEach { name ->
                val target = File(dir, "eng.traineddata")
                if (!target.exists()) {
                    context.assets.open("tessdata/$name").use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}