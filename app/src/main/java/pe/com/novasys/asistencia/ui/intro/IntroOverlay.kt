package pe.com.novasys.asistencia.ui.intro

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pe.com.novasys.asistencia.ui.components.OrbitEmblem
import pe.com.novasys.asistencia.ui.theme.NovaAccent
import pe.com.novasys.asistencia.ui.theme.NovaTheme

/**
 * Cold-start intro. Picks up where the system splash leaves off (same emblem,
 * same spot), sketches the mark, brings in the wordmark, then lifts away to
 * reveal the app, which has been loading underneath the whole time.
 */
@Composable
fun IntroOverlay(onFinished: () -> Unit) {
    val p = NovaTheme.glass
    val draw = remember { Animatable(0f) }
    val orbit = remember { Animatable(0f) }
    val word = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { orbit.animateTo(220f, tween(1_700, easing = LinearEasing)) }
            val sketch = async { draw.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            delay(450)
            word.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            sketch.await()
            delay(250)
            exit.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - exit.value }
            .background(p.canvasTop),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.graphicsLayer {
                val s = 1f + 0.12f * exit.value
                scaleX = s
                scaleY = s
            },
        ) {
            OrbitEmblem(
                size = 160.dp,
                ringColor = p.textPrimary.copy(alpha = 0.55f),
                accent = NovaAccent,
                starColor = p.textPrimary,
                draw = draw.value,
                orbit = orbit.value,
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = word.value
                    translationY = (1f - word.value) * 16.dp.toPx()
                },
            ) {
                Text(
                    "NOVA",
                    color = p.textPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    // Letters settle together as the word fades in.
                    letterSpacing = (10f - 4f * word.value).sp,
                )
                Text(
                    "ASSISTANCE",
                    color = p.accentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 6.sp,
                )
            }
        }
    }
}
