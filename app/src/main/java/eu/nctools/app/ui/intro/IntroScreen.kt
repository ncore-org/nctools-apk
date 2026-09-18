package eu.nctools.app.ui.intro

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val BackOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Branded, animated launch intro.
 *  - Living gradient (blue → violet) with orbiting light rings and drifting orbs.
 *  - A monogram tile that springs in, then the wordmark types in per-letter.
 *  - A three-dot "loading" indicator that blinks like a loader.
 * On completion, hands off to the app.
 */
@Composable
fun IntroScreen(onFinished: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val start = Color(0xFF2563EB)
    val end = Color(0xFF7C3AED)

    // Continuous background motion.
    val bg = rememberInfiniteTransition(label = "bg")
    val drift by bg.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val spin by bg.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    val ringPulse by bg.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring",
    )

    // Entrance choreography.
    var show by remember { mutableStateOf(false) }
    val heroScale by animateFloatAsState(
        if (show) 1f else 0.4f,
        tween(700, easing = BackOut), label = "heroScale",
    )
    val heroAlpha by animateFloatAsState(if (show) 1f else 0f, tween(550), label = "heroAlpha")
    val wordAlpha by animateFloatAsState(if (show) 1f else 0f, tween(700, delayMillis = 250), label = "wordAlpha")
    val wordSlide by animateFloatAsState(if (show) 1f else 0f, tween(700, delayMillis = 250, easing = BackOut), label = "wordSlide")
    val subAlpha by animateFloatAsState(if (show) 1f else 0f, tween(700, delayMillis = 550), label = "subAlpha")
    val dotsAlpha by animateFloatAsState(if (show) 1f else 0f, tween(600, delayMillis = 800), label = "dotsAlpha")

    LaunchedEffect(Unit) {
        show = true
        delay(2000)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
        // Animovaný background: kroužky + orbly.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Pulzujúce svetelné kružnice okolo stredu (dýchajúc).
            for (i in 0..2) {
                val base = w * (0.28f + i * 0.13f)
                val r = base + ringPulse * w * 0.05f
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f - i * 0.02f),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }

            // Dva ručne otočené arc segmenty (rotujúce).
            val segStart = spin.toFloat() * (Math.PI / 180).toFloat()
            val sweep = (Math.PI * 1.35).toFloat()
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = 0f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - w * 0.24f, cy - w * 0.24f),
                size = androidx.compose.ui.geometry.Size(w * 0.48f, w * 0.48f),
                style = Stroke(width = 2.dp.toPx()),
            )
            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 180f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - w * 0.33f, cy - w * 0.33f),
                size = androidx.compose.ui.geometry.Size(w * 0.66f, w * 0.66f),
                style = Stroke(width = 1.5.dp.toPx()),
            )

            // Drifting orbs (pozadie).
            drawCircle(Color.White.copy(alpha = 0.07f), w * 0.4f, Offset(w * (0.12f + drift * 0.18f), h * 0.16f))
            drawCircle(Color.White.copy(alpha = 0.05f), w * 0.28f, Offset(w * (0.86f - drift * 0.14f), h * 0.82f))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Monogram tile.
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer { scaleX = heroScale; scaleY = heroScale; alpha = heroAlpha }
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0x40FFFFFF), Color(0x0DFFFFFF), Color(0x33FFFFFF))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("n", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(28.dp))

            // Wordmark with per-letter reveal + spring slide.
            Row(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = wordAlpha
                        translationY = (1f - wordSlide) * 30f
                        scaleX = wordSlide.coerceAtLeast(0.4f)
                        scaleY = wordSlide.coerceAtLeast(0.4f)
                    },
                horizontalArrangement = Arrangement.Center,
            ) {
                "nctools".forEachIndexed { i, ch ->
                    val l = wordSlide.coerceIn(0f, 1f)
                    val startT = (l - i * 0.06f).coerceIn(0f, 1f)
                    Text(
                        text = ch.toString(),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = startT
                                translationY = (1f - startT) * 18f
                            },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                "Private document tools",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(subAlpha)
                    .graphicsLayer { translationY = (1f - subAlpha) * 14f },
            )

            Spacer(Modifier.height(34.dp))

            // Loading dots — blink like a loader (staggered).
            LoadingDots(alpha = dotsAlpha)
        }
    }
}

@Composable
private fun LoadingDots(alpha: Float) {
    val t = rememberInfiniteTransition(label = "dots")
    val phases = listOf(
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(400, delayMillis = 0), RepeatMode.Restart), label = "d0"),
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(400, delayMillis = 200), RepeatMode.Restart), label = "d1"),
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(400, delayMillis = 400), RepeatMode.Restart), label = "d2"),
    )
    Row(
        modifier = Modifier.alpha(alpha),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        phases.forEach { p ->
            // Triangle wave: rises then falls → each dot blinks on/off.
            val blink = (1f - kotlin.math.abs(2 * p.value - 1f))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        this.alpha = (0.25f + 0.75f * blink).coerceIn(0.2f, 1f)
                        scaleX = 0.7f + 0.5f * blink
                        scaleY = 0.7f + 0.5f * blink
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
            )
        }
    }
}