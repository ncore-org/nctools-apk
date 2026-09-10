package eu.nctools.app.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import eu.nctools.app.core.tools.ToolsEngine.Tool

/**
 * Presentation metadata for each tool — icon + accent colour, used by the
 * dashboard cards. Kept in the UI layer so the tool engine stays domain-only.
 */
data class ToolVisual(val icon: ImageVector, val tint: Color)

object ToolVisuals {
    private val BLUE = Color(0xFF2563EB)
    private val VIOLET = Color(0xFF7C3AED)
    private val EMERALD = Color(0xFF059669)
    private val AMBER = Color(0xFFD97706)
    private val ROSE = Color(0xFFE11D48)
    private val CYAN = Color(0xFF0891B2)
    private val INDIGO = Color(0xFF4F46E5)
    private val TEAL = Color(0xFF0D9488)

    private val map: Map<String, ToolVisual> = mapOf(
        "paste-to-pdf" to ToolVisual(Icons.Filled.ContentPasteGo, INDIGO),
        "pdf-to-word" to ToolVisual(Icons.Filled.MenuBook, BLUE),
        "pdf-to-excel" to ToolVisual(Icons.Filled.GridOn, EMERALD),
        "pdf-to-ocr" to ToolVisual(Icons.Filled.FindInPage, VIOLET),
        "ocr-to-text" to ToolVisual(Icons.Filled.TextFields, CYAN),
        "photo-scanner" to ToolVisual(Icons.Filled.DocumentScanner, AMBER),
        "images-to-pdf" to ToolVisual(Icons.Filled.PhotoLibrary, ROSE),
        "merge-pdf" to ToolVisual(Icons.Filled.CallMerge, TEAL),
    )

    private val fallback = ToolVisual(Icons.Filled.TextSnippet, BLUE)

    fun of(slug: String): ToolVisual = map[slug] ?: fallback
    fun of(tool: Tool): ToolVisual = of(tool.slug)
}