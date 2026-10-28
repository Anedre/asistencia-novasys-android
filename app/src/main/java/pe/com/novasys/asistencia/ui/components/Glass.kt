package pe.com.novasys.asistencia.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import pe.com.novasys.asistencia.ui.theme.NovaTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The backdrop every glass surface blurs. Provided once at the root; a surface
 * outside a provider falls back to a plain translucent fill.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Slow-drifting "aurora" canvas: a vertical wash plus three soft light blobs.
 * It is what gives the glass something to refract — on a flat colour, blur
 * has nothing to show.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val p = NovaTheme.glass
    val drift by rememberInfiniteTransition(label = "aurora").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )

    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(p.canvasTop, p.canvasBottom)))

        val w = size.width
        val h = size.height
        val a = drift * 2f * PI.toFloat()

        fun blob(color: Color, x: Float, y: Float, radius: Float) {
            val center = Offset(x, y)
            drawCircle(
                brush = Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), center, radius),
                radius = radius,
                center = center,
            )
        }

        blob(p.auroraAccent, w * (0.20f + 0.18f * cos(a)), h * (0.14f + 0.05f * sin(a)), w * 0.85f)
        blob(p.auroraCool, w * (0.95f - 0.15f * sin(a)), h * (0.42f + 0.08f * cos(a)), w * 0.90f)
        blob(p.auroraNeutral, w * (0.15f + 0.12f * sin(a)), h * (0.88f - 0.06f * cos(a)), w * 0.95f)
    }
}

/**
 * A liquid-glass panel: real backdrop blur (Android 12+; tint fallback below),
 * a translucent tint, a top-lit edge and a specular sheen on the upper third.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    strong: Boolean = false,
    tint: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = NovaTheme.glass
    val haze = LocalHazeState.current
    val fill = tint ?: if (strong) p.glassFillStrong else p.glassFill

    val surface = if (haze != null) {
        Modifier.hazeEffect(
            state = haze,
            style = HazeStyle(
                backgroundColor = p.canvasTop,
                tint = HazeTint(fill),
                blurRadius = 30.dp,
                noiseFactor = 0.035f,
            ),
        )
    } else {
        Modifier.background(fill)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(surface)
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(
                        0f to p.specular,
                        0.38f to Color.Transparent,
                    )
                )
            }
            .border(1.dp, Brush.verticalGradient(listOf(p.glassEdgeTop, p.glassEdgeBottom)), shape),
        content = content,
    )
}

enum class GlassButtonStyle { Primary, Glass }

/** Pill button that gives under the finger with a springy squash. */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 56.dp,
) {
    val p = NovaTheme.glass
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    val alpha by animateFloatAsState(if (enabled || loading) 1f else 0.45f, label = "enabled")
    val shape = RoundedCornerShape(percent = 50)
    val contentColor = if (style == GlassButtonStyle.Primary) p.onInk else p.textPrimary

    val body: @Composable BoxScope.() -> Unit = {
        AnimatedContent(
            targetState = loading,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(tween(120))
            },
            label = "button-content",
            modifier = Modifier.align(Alignment.Center),
        ) { busy ->
            if (busy) {
                OrbitLoader(size = 24.dp, color = contentColor)
            } else {
                Text(text, color = contentColor, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }

    val base = modifier
        .height(height)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled && !loading,
            role = Role.Button,
            onClick = onClick,
        )

    when (style) {
        GlassButtonStyle.Primary -> Box(
            modifier = base
                .clip(shape)
                .background(Brush.verticalGradient(listOf(p.ink.copy(alpha = 0.92f), p.ink)))
                .border(1.dp, Brush.verticalGradient(listOf(p.glassEdgeTop.copy(alpha = 0.35f), Color.Transparent)), shape),
            content = body,
        )
        GlassButtonStyle.Glass -> GlassSurface(modifier = base, shape = shape, strong = true, content = body)
    }
}

/** Round glass button for a glyph (theme toggle, sign-out). */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    glyph: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "icon-press",
    )
    GlassSurface(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        shape = CircleShape,
        strong = true,
    ) {
        Box(Modifier.align(Alignment.Center), content = glyph)
    }
}

/** Status capsule; [live] makes the dot breathe (e.g. while a shift is open). */
@Composable
fun GlassPill(text: String, tone: Color, modifier: Modifier = Modifier, live: Boolean = false) {
    val pulse by rememberInfiniteTransition(label = "pill").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_100), RepeatMode.Reverse),
        label = "pulse",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.14f))
            .border(1.dp, tone.copy(alpha = 0.22f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(7.dp)
                .graphicsLayer { alpha = if (live) pulse else 1f }
                .background(tone, CircleShape)
        )
        Spacer(Modifier.width(7.dp))
        Text(text, color = tone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Thin glowing progress track used inside glass cards. */
@Composable
fun GlassProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val p = NovaTheme.glass
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessVeryLow),
        label = "progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(p.textPrimary.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(8.dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)))
        )
    }
}
