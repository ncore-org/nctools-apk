package eu.nctools.app.ui.intro

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Branded, animated launch intro. Shown once on cold start, then hands off to
 * the app. Living gradient background + an animated wordmark, all built with
 * Compose animations (no bitmap assets).
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val bg = MaterialTheme.colorScheme.primary
    val bgDark = MaterialTheme.colorScheme.primary.copy(red = 0.11f, green = 0.25f, blue = 0.72f)

    // Continuous background motion (orbs drift + pulse forever).
    val infinite = rememberInfiniteTransition(label = "intro-bg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Entrance choreography.
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1900)
        onFinished()
    }
    val logoScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoScale",
    )
    val logoAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "logoAlpha",
    )
    val taglineAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900, delayMillis = 500),
        label = "taglineAlpha",
    )
    val dotAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(900, delayMillis = 800),
        label = "dotAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg, bgDark))),
        contentAlignment = Alignment.Center,
    ) {
        // Soft drifting orbs in the background.
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = w * 0.42f,
                center = Offset(w * (0.15f + drift * 0.12f), h * 0.18f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = w * 0.30f,
                center = Offset(w * (0.88f - drift * 0.10f), h * 0.80f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = w * 0.20f,
                center = Offset(w * 0.7f, h * (0.30f + drift * 0.08f)),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App mark — rounded tile with a monogram, gently pulsing.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(logoScale * pulse.coerceAtMost(1.02f))
                    .alpha(logoAlpha)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "n",
                    color = Color.White,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(26.dp))

            // Animated wordmark.
            Text(
                text = "nctools",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = logoAlpha
                    translationY = (1f - logoAlpha) * 24f
                },
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Private document tools",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha)
                    .graphicsLayer { translationY = (1f - taglineAlpha) * 16f },
            )

            Spacer(Modifier.height(30.dp))

            // Three pulsing loading dots.
            Box(modifier = Modifier.alpha(dotAlpha)) {
                Text("•  •  •", color = Color.White.copy(alpha = 0.7f), fontSize = 20.sp)
            }
        }
    }
}