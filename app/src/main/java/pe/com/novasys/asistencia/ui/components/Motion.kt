package pe.com.novasys.asistencia.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Nova orbital mark (two rings, a four-point star, orbiting dots) drawn in
 * code so it can animate and recolour with the theme.
 *
 * [draw] runs 0→1 to sketch it in (the intro); [orbit] is an angle in degrees
 * the dots travel around their rings.
 */
@Composable
fun OrbitEmblem(
    size: Dp,
    ringColor: Color,
    accent: Color,
    starColor: Color,
    modifier: Modifier = Modifier,
    draw: Float = 1f,
    orbit: Float = 0f,
) {
    Canvas(modifier.size(size)) {
        val c = center
        val unit = this.size.minDimension / 108f
        val outer = 27f * unit
        val inner = 17f * unit
        val stroke = 1.6f * unit

        // Rings sketch in clockwise from the top.
        val ringSweep = (draw * 1.6f).coerceIn(0f, 1f) * 360f
        drawArc(
            color = ringColor, startAngle = -90f, sweepAngle = ringSweep, useCenter = false,
            topLeft = Offset(c.x - outer, c.y - outer),
            size = androidx.compose.ui.geometry.Size(outer * 2, outer * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = accent, startAngle = 90f, sweepAngle = ringSweep, useCenter = false,
            topLeft = Offset(c.x - inner, c.y - inner),
            size = androidx.compose.ui.geometry.Size(inner * 2, inner * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )

        // Star pops in once the rings are mostly drawn.
        val starT = ((draw - 0.45f) / 0.55f).coerceIn(0f, 1f)
        if (starT > 0f) {
            val r = 13f * unit * starT
            val k = 2.2f * unit * starT
            val star = Path().apply {
                moveTo(c.x, c.y - r)
                lineTo(c.x + k, c.y - k); lineTo(c.x + r, c.y); lineTo(c.x + k, c.y + k)
                lineTo(c.x, c.y + r); lineTo(c.x - k, c.y + k); lineTo(c.x - r, c.y); lineTo(c.x - k, c.y - k)
                close()
            }
            drawPath(star, starColor)
            drawCircle(accent, radius = 2.6f * unit * starT, center = c)
        }

        // Orbiting dots.
        val dotsT = ((draw - 0.7f) / 0.3f).coerceIn(0f, 1f)
        if (dotsT > 0f) {
            val a1 = ((orbit - 35f) * PI / 180f).toFloat()
            val a2 = ((-orbit * 1.6f + 70f) * PI / 180f).toFloat()
            drawCircle(accent.copy(alpha = dotsT), 2.4f * unit, Offset(c.x + outer * cos(a1), c.y + outer * sin(a1)))
            drawCircle(accent.copy(alpha = dotsT), 2.0f * unit, Offset(c.x + inner * cos(a2), c.y + inner * sin(a2)))
        }
    }
}

/** The emblem idling: dots circling forever. */
@Composable
fun OrbitingEmblem(size: Dp, ringColor: Color, accent: Color, starColor: Color, modifier: Modifier = Modifier) {
    val orbit by rememberInfiniteTransition(label = "emblem").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9_000, easing = LinearEasing)),
        label = "orbit",
    )
    OrbitEmblem(size, ringColor, accent, starColor, modifier, draw = 1f, orbit = orbit)
}

/** Loading indicator in the emblem's language: a comet arc chasing a dot. */
@Composable
fun OrbitLoader(size: Dp, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loader")
    val spin by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(950, easing = LinearEasing)), label = "spin",
    )
    val breathe by transition.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "breathe",
    )
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.11f
        val r = (this.size.minDimension - stroke) / 2f
        drawCircle(color.copy(alpha = 0.18f), radius = r, style = Stroke(stroke))
        rotate(spin) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, color)),
                startAngle = 0f, sweepAngle = 300f, useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            drawCircle(color, radius = stroke * 0.85f * breathe, center = Offset(center.x + r, center.y))
        }
    }
}

/**
 * Entrance for a list of sections: each rises and fades in a beat after the
 * one before it. Plays once per composition of the element.
 */
fun Modifier.staggerIn(index: Int, baseDelayMillis: Long = 60L): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * baseDelayMillis)
        progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
    }
    graphicsLayer {
        val v = progress.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 32.dp.toPx()
        val s = 0.96f + 0.04f * v
        scaleX = s
        scaleY = s
    }
}

/** A light band sweeping across placeholder content while it loads. */
fun Modifier.shimmer(highlight: Color): Modifier = composed {
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing)),
        label = "sweep",
    )
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val w = size.width
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(w * x - w * 0.4f, 0f),
                    end = Offset(w * x + w * 0.4f, size.height),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        }
}

/**
 * Text whose changed characters roll in from below, like a split-flap display.
 * Unchanged characters stay still, so a ticking clock only animates the digits
 * that actually move.
 */
@Composable
fun RollingText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    Row(modifier) {
        text.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(320))) togetherWith
                        (slideOutVertically(tween(320)) { -it / 2 } + fadeOut(tween(200)))
                },
                label = "roll-$index",
            ) { c ->
                Text(c.toString(), style = style, color = color)
            }
        }
    }
}
