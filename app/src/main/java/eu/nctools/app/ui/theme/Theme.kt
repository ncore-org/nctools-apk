package eu.nctools.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// nctools brand palette — matches the website (green / white / dark green).
private val Primary = Color(0xFF16A34A)
private val PrimaryDark = Color(0xFF15803D)
private val SurfaceLight = Color(0xFFFFFFFF)
private val BackgroundLight = Color(0xFFF7F9F8)
private val TextPrimary = Color(0xFF0F1A15)
private val TextMuted = Color(0xFF6B7C74)
private val SurfaceDark = Color(0xFF121A16)
private val BackgroundDark = Color(0xFF0B120E)
private val TextPrimaryDark = Color(0xFFE9F2ED)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = PrimaryDark,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    secondary = Color(0xFF4ADE80),
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = Color(0xFFF87171),
)

@Composable
fun NctoolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}