package eu.nctools.app.core.tools

import eu.nctools.app.core.tools.ToolsEngine.Tool
import eu.nctools.app.core.tools.ToolsEngine.ToolCategory

/** Static registry of all tools — mirrors the nctools.eu catalog. */
object ToolCatalog {
    val all: List<Tool> = listOf(
        Tool("paste-to-pdf", "Paste to PDF", "Text to PDF", ToolCategory.CREATE),
        Tool("pdf-to-word", "PDF to Word", "PDF → DOCX", ToolCategory.CONVERT),
        Tool("pdf-to-excel", "PDF to Excel", "PDF → XLSX", ToolCategory.CONVERT),
        Tool("pdf-to-ocr", "PDF to OCR", "PDF → searchable", ToolCategory.CONVERT),
        Tool("ocr-to-text", "OCR to Text", "Image → text", ToolCategory.CONVERT),
        Tool("photo-scanner", "Photo Scanner", "Camera → PDF", ToolCategory.CREATE),
        Tool("images-to-pdf", "Images to PDF", "PNG/JPG → PDF", ToolCategory.CREATE),
        Tool("merge-pdf", "Merge PDF", "N files → one", ToolCategory.CONVERT),
    )

    fun bySlug(slug: String): Tool? = all.find { it.slug == slug }
}