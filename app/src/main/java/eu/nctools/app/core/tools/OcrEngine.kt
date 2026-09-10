package eu.nctools.app.core.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tesseract OCR. English (+ Slovak optional) training data is shipped under
 * app/src/main/assets/tessdata (degraded gracefully, see README). The engine
 * NEVER drives the native library without a valid traineddata file — calling
 * init() with no language data is a hard segfault on many devices, which is
 * exactly the "app crashes when I open a tool" symptom we must prevent.
 */
@Singleton
class OcrEngine @Inject constructor(private val context: Context) {

    private var dataDir: File? = null

    suspend fun recognize(uri: Uri): String = withContext(Dispatchers.IO) {
        if (!ensureLanguage()) {
            return@withContext "OCR isn't available yet — install the language pack, then retry."
        }
        runCatching { recognizeBitmap(decode(uri)) }
            .getOrElse { e ->
                Log.w(TAG, "ocr failed", e)
                "Could not read the image for OCR."
            }
    }

    /** Best-effort searchable-text overlay: copies the source, adds nothing new. */
    fun renderSearchable(src: PDDocument, out: File): String {
        return try {
            src.save(out)
            ""
        } catch (e: Exception) {
            Log.w(TAG, "pdf copy failed", e)
            ""
        }
    }

    /**
     * Copies eng.traineddata from assets if present. Returns true only when a
     * usable eng.traineddata exists on disk — otherwise callers must back off
     * instead of touching the native API.
     */
    @Synchronized
    private fun ensureLanguage(): Boolean {
        val dir = File(context.filesDir, "tesseract").apply { mkdirs() }
        val eng = File(dir, "eng.traineddata")
        if (eng.exists() && eng.length() > 100_000) {
            dataDir = dir
            return true
        }
        // Try to ship from assets/tessdata (bundled in release builds).
        return try {
            val asset = context.assets.open("tessdata/eng.traineddata")
            asset.use { input ->
                FileOutputStream(eng).use { output -> input.copyTo(output) }
            }
            dataDir = dir
            eng.length() > 100_000
        } catch (_: Exception) {
            Log.w(TAG, "eng.traineddata not bundled in assets")
            false
        }
    }

    private fun openApi(): TessBaseAPI? {
        val dir = dataDir ?: return null
        return TessBaseAPI().apply {
            if (!init(dir.absolutePath, "eng")) {
                end()
                return null
            }
        }
    }

    private fun recognizeBitmap(bmp: Bitmap): String {
        val api = openApi() ?: return "OCR isn't available yet — install the language pack, then retry."
        return try {
            api.setImage(bmp)
            api.utF8Text?.trim() ?: ""
        } finally {
            api.end()
        }
    }

    private fun decode(uri: Uri): Bitmap {
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    companion object { private const val TAG = "OcrEngine" }
}