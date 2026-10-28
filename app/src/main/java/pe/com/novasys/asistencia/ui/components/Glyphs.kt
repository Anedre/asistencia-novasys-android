package pe.com.novasys.asistencia.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sun that morphs into a moon: the rays retract and spin away while a second
 * disc slides in to bite the crescent out.
 */
@Composable
fun SunMoonGlyph(dark: Boolean, color: Color, size: Dp = 20.dp) {
    val t by animateFloatAsState(
        targetValue = if (dark) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "sun-moon",
    )
    Canvas(
        Modifier
            .size(size)
            // Offscreen so the "bite" can erase only the glyph, not what's behind it.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        val c = center
        val r = this.size.minDimension * (0.24f + 0.10f * t)

        rotate(degrees = 90f * t) {
            val rayIn = this@Canvas.size.minDimension * 0.36f
            val rayOut = this@Canvas.size.minDimension * 0.48f
            val rayAlpha = (1f - t * 1.6f).coerceIn(0f, 1f)
            for (i in 0 until 8) {
                val a = (i * 45.0 * PI / 180.0).toFloat()
                drawLine(
                    color = color.copy(alpha = color.alpha * rayAlpha),
                    start = Offset(c.x + rayIn * cos(a), c.y + rayIn * sin(a)),
                    end = Offset(c.x + rayOut * cos(a), c.y + rayOut * sin(a)),
                    strokeWidth = this@Canvas.size.minDimension * 0.08f,
                    cap = StrokeCap.Round,
                )
            }
        }

        drawCircle(color, radius = r, center = c)
        if (t > 0f) {
            drawCircle(
                color = Color.Black,
                radius = r * 0.92f,
                center = Offset(c.x + r * (1.5f - 0.75f * t), c.y - r * (1.1f - 0.6f * t)),
                blendMode = BlendMode.DstOut,
            )
        }
    }
}

/** Sign-out: an open ring with a bar through the gap (a power glyph). */
@Composable
fun SignOutGlyph(color: Color, size: Dp = 18.dp) {
    Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.11f
        val r = this.size.minDimension * 0.38f
        drawArc(
            color = color,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r + stroke * 0.4f),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = Offset(center.x, center.y - r - stroke * 0.2f),
            end = Offset(center.x, center.y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
