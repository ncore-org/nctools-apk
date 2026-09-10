package eu.nctools.app.core.tools

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import eu.nctools.app.core.update.UpdateManager
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * On-device document conversion engine. Mirrors every nctools.eu tool:
 * paste-to-pdf, pdf-to-word, pdf-to-excel, pdf-to-ocr, ocr-to-text,
 * photo-scanner, images-to-pdf, merge-pdf. All conversions stay on device —
 * zero uploads, matching the privacy promise of the website.
 *
 * Each public operation is intended to be wrapped by AdGate (one 15s ad per
 * interaction); the tool itself is pure and side-effect free.
 */
@Singleton
class ToolsEngine @Inject constructor(
    private val context: Context,
    private val ocr: OcrEngine,
) {
    val tools: List<Tool> = ToolCatalog.all

    enum class ToolCategory { CREATE, CONVERT }

    data class Tool(val slug: String, val title: String, val subtitle: String, val category: ToolCategory)

    data class Result(val output: File, val mime: String, val fileName: String)

    /** paste-to-pdf */
    suspend fun pasteToPdf(text: String): Result = withContext(Dispatchers.IO) {
        val doc = PDDocument()
        try {
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            val stream = PDPageContentStream(doc, page)
            stream.beginText()
            stream.setFont(com.tom_roush.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
            stream.newLineAtOffset(50f, 750f)
            text.split("\n").forEach { line ->
                stream.showText(line.take(120))
                stream.newLineAtOffset(0f, -16f)
            }
            stream.endText()
            stream.close()
            val file = File(context.cacheDir, "paste-to-pdf.pdf")
            doc.save(file)
            Result(file, "application/pdf", "document.pdf")
        } finally { doc.close() }
    }

    /** pdf-to-word — extracts text to a DOCX container */
    suspend fun pdfToWord(pdf: Uri): Result = withContext(Dispatchers.IO) {
        val text = extractText(pdf)
        val file = File(context.cacheDir, "document.docx")
        file.outputStream().use { out ->
            writeDocx(out, text)
        }
        Result(file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "document.docx")
    }

    /** pdf-to-excel — extracts text lines as CSV rows in an XLSX container */
    suspend fun pdfToExcel(pdf: Uri): Result = withContext(Dispatchers.IO) {
        val text = extractText(pdf)
        val file = File(context.cacheDir, "document.xlsx")
        file.outputStream().use { out ->
            writeXlsx(out, text)
        }
        Result(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "document.xlsx")
    }

    /** pdf-to-ocr — adds a searchable text layer via OCR of rendered pages */
    suspend fun pdfToOcr(pdf: Uri): Result = withContext(Dispatchers.IO) {
        // Reuse the source PDF; OCR layer is applied as an overlay text.
        val text = extractText(pdf)
        val file = File(context.cacheDir, "document-ocr.pdf")
        context.contentResolver.openInputStream(pdf)?.use { input ->
            val src = com.tom_roush.pdfbox.pdmodel.PDDocument.load(
                java.io.ByteArrayInputStream(input.readBytes())
            )
            try {
                if (text.isBlank()) {
                    // No embedded text → OCR each page bitmap.
                    ocr.renderSearchable(src, file)
                } else {
                    src.save(file)
                }
            } finally { src.close() }
        }
        Result(file, "application/pdf", "document-ocr.pdf")
    }

    /** ocr-to-text — bitmap/photo → text via Tesseract */
    suspend fun ocrToText(image: Uri): Result = withContext(Dispatchers.IO) {
        val text = ocr.recognize(image)
        val file = File(context.cacheDir, "text.txt")
        file.writeText(text)
        Result(file, "text/plain", "text.txt")
    }

    /** photo-scanner — camera bitmap → clean PDF scan */
    suspend fun photoScanner(bitmap: Bitmap): Result = withContext(Dispatchers.IO) {
        val doc = PDDocument()
        try {
            val page = PDPage(PDRectangle.A4)
            doc.addPage(page)
            val stream = PDPageContentStream(doc, page)
            val scaled = if (bitmap.width > 1200) {
                Bitmap.createScaledBitmap(bitmap, 1200, (1200f * bitmap.height / bitmap.width).toInt(), true)
            } else bitmap
            stream.drawImage(toJpeg(doc, scaled), 40f, 40f, page.mediaBox.width - 80f, page.mediaBox.height - 80f)
            stream.close()
            val file = File(context.cacheDir, "scan.pdf")
            doc.save(file)
            Result(file, "application/pdf", "scan.pdf")
        } finally { doc.close() }
    }

    /** images-to-pdf — a list of photos → a single PDF */
    suspend fun imagesToPdf(images: List<Bitmap>): Result = withContext(Dispatchers.IO) {
        val doc = PDDocument()
        try {
            images.forEach { bmp ->
                val page = PDPage(PDRectangle.A4)
                doc.addPage(page)
                val stream = PDPageContentStream(doc, page)
                stream.drawImage(toJpeg(doc, bmp), 40f, 40f, page.mediaBox.width - 80f, page.mediaBox.height - 80f)
                stream.close()
            }
            val file = File(context.cacheDir, "images.pdf")
            doc.save(file)
            Result(file, "application/pdf", "images.pdf")
        } finally { doc.close() }
    }

    /** merge-pdf — several PDFs into one */
    suspend fun mergePdf(sources: List<Uri>): Result = withContext(Dispatchers.IO) {
        val merger = PDFMergerUtility()
        sources.forEach { merger.addSource(it.toString()) }
        val file = File(context.cacheDir, "merged.pdf")
        merger.destinationStream = FileOutputStream(file)
        merger.mergeDocuments(null)
        Result(file, "application/pdf", "merged.pdf")
    }

    private fun extractText(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return ""
        return com.tom_roush.pdfbox.pdmodel.PDDocument.load(
            java.io.ByteArrayInputStream(bytes)
        ).use { PDFTextStripper().getText(it) }
    }

    /** Serialize a Bitmap to a documented JPEG stream for pdfbox. */
    private fun toJpeg(doc: PDDocument, bmp: Bitmap): com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject {
        val bos = java.io.ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, bos)
        return JPEGFactory.createFromStream(doc, java.io.ByteArrayInputStream(bos.toByteArray()))
    }

    // --- minimal DOCX / XLSX writers (OPC zip containers) ---
    private fun writeDocx(out: FileOutputStream, text: String) {
        val zip = java.util.zip.ZipOutputStream(out)
        fun entry(name: String, body: String) {
            zip.putNextEntry(java.util.zip.ZipEntry(name))
            zip.write(body.toByteArray())
            zip.closeEntry()
        }
        entry("_rels/.rels", """<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>""")
        entry("[Content_Types].xml", """<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/></Types>""")
        val paras = text.split("\n").filter { it.isNotBlank() }
            .joinToString("") { "<w:p><w:r><w:t xml:space=\"preserve\">${it.trim().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</w:t></w:r></w:p>" }
        entry("word/document.xml", """<?xml version="1.0"?><w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$paras</w:body></w:document>""")
        zip.close()
    }

    private fun writeXlsx(out: FileOutputStream, text: String) {
        val rows = text.split("\n").filter { it.isNotBlank() }.map { it.trim() }
        val sheet = StringBuilder()
        sheet.append("""<?xml version="1.0"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEach { row ->
            sheet.append("<row>")
            row.split(Regex("\\s{2,}|\\t")).forEach { cell ->
                sheet.append("<c t=\"inlineStr\"><is><t>").append(cell.trim().replace("&", "&amp;").replace("<", "&lt;"))
                    .append("</t></is></c>")
            }
            sheet.append("</row>")
        }
        sheet.append("</sheetData></worksheet>")
        val zip = java.util.zip.ZipOutputStream(out)
        fun entry(name: String, body: String) {
            zip.putNextEntry(java.util.zip.ZipEntry(name)); zip.write(body.toByteArray()); zip.closeEntry()
        }
        entry("_rels/.rels", """<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
        entry("[Content_Types].xml", """<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""")
        entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""")
        entry("xl/workbook.xml", """<?xml version="1.0"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/></sheets></workbook>""")
        entry("xl/worksheets/sheet1.xml", sheet.toString())
        zip.close()
    }
}