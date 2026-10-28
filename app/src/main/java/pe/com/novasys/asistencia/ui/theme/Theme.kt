package pe.com.novasys.asistencia.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Brand accent shared with the web (nova-design.css `--accent`). */
val NovaAccent = Color(0xFF3FBEFF)

enum class ThemeMode { System, Light, Dark }

/**
 * Every colour the liquid-glass UI draws with.
 *
 * Light runs from pure white into soft silver; dark is jet black ("azabache")
 * with graphite and a faint blue cast. Both palettes have the same slots so
 * the theme can interpolate between them when the mode flips, instead of
 * snapping.
 */
@Immutable
data class GlassPalette(
    val canvasTop: Color,
    val canvasBottom: Color,
    val auroraAccent: Color,
    val auroraCool: Color,
    val auroraNeutral: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassEdgeTop: Color,
    val glassEdgeBottom: Color,
    val specular: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    /** Primary-action fill: jet on light, near-white on dark (iOS style). */
    val ink: Color,
    val onInk: Color,
    /** Accent tuned for legibility as text on this canvas. */
    val accentText: Color,
    val success: Color,
    val warn: Color,
    val danger: Color,
)

private val LightGlass = GlassPalette(
    canvasTop = Color(0xFFFFFFFF),
    canvasBottom = Color(0xFFEDEFF3),
    auroraAccent = NovaAccent.copy(alpha = 0.26f),
    auroraCool = Color(0xFFC9D4E6).copy(alpha = 0.70f),
    auroraNeutral = Color(0xFFE4E6EC).copy(alpha = 0.90f),
    glassFill = Color.White.copy(alpha = 0.52f),
    glassFillStrong = Color.White.copy(alpha = 0.72f),
    glassEdgeTop = Color.White.copy(alpha = 0.95f),
    glassEdgeBottom = Color(0xFF0B0B0D).copy(alpha = 0.07f),
    specular = Color.White.copy(alpha = 0.55f),
    textPrimary = Color(0xFF0B0B0D),
    textSecondary = Color(0xFF4A4E57),
    textMuted = Color(0xFF8B9099),
    ink = Color(0xFF0B0B0D),
    onInk = Color.White,
    accentText = Color(0xFF0A86C8),
    success = Color(0xFF0E9F6E),
    warn = Color(0xFFC77A05),
    danger = Color(0xFFE11D48),
)

private val DarkGlass = GlassPalette(
    canvasTop = Color(0xFF050506),
    canvasBottom = Color(0xFF0D0E11),
    auroraAccent = NovaAccent.copy(alpha = 0.16f),
    auroraCool = Color(0xFF18202E).copy(alpha = 0.95f),
    auroraNeutral = Color(0xFF202126).copy(alpha = 0.85f),
    glassFill = Color.White.copy(alpha = 0.055f),
    glassFillStrong = Color.White.copy(alpha = 0.09f),
    glassEdgeTop = Color.White.copy(alpha = 0.22f),
    glassEdgeBottom = Color.White.copy(alpha = 0.04f),
    specular = Color.White.copy(alpha = 0.07f),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFA3A6AF),
    textMuted = Color(0xFF61656E),
    ink = Color(0xFFF5F5F7),
    onInk = Color(0xFF0B0B0D),
    accentText = Color(0xFF5CCBFF),
    success = Color(0xFF34D399),
    warn = Color(0xFFFBBF24),
    danger = Color(0xFFFB7185),
)

private fun lerp(a: GlassPalette, b: GlassPalette, t: Float) = GlassPalette(
    canvasTop = lerp(a.canvasTop, b.canvasTop, t),
    canvasBottom = lerp(a.canvasBottom, b.canvasBottom, t),
    auroraAccent = lerp(a.auroraAccent, b.auroraAccent, t),
    auroraCool = lerp(a.auroraCool, b.auroraCool, t),
    auroraNeutral = lerp(a.auroraNeutral, b.auroraNeutral, t),
    glassFill = lerp(a.glassFill, b.glassFill, t),
    glassFillStrong = lerp(a.glassFillStrong, b.glassFillStrong, t),
    glassEdgeTop = lerp(a.glassEdgeTop, b.glassEdgeTop, t),
    glassEdgeBottom = lerp(a.glassEdgeBottom, b.glassEdgeBottom, t),
    specular = lerp(a.specular, b.specular, t),
    textPrimary = lerp(a.textPrimary, b.textPrimary, t),
    textSecondary = lerp(a.textSecondary, b.textSecondary, t),
    textMuted = lerp(a.textMuted, b.textMuted, t),
    ink = lerp(a.ink, b.ink, t),
    onInk = lerp(a.onInk, b.onInk, t),
    accentText = lerp(a.accentText, b.accentText, t),
    success = lerp(a.success, b.success, t),
    warn = lerp(a.warn, b.warn, t),
    danger = lerp(a.danger, b.danger, t),
)

private val LocalGlass = staticCompositionLocalOf { LightGlass }
private val LocalIsDark = staticCompositionLocalOf { false }

object NovaTheme {
    val glass: GlassPalette
        @Composable get() = LocalGlass.current

    /** The mode being shown (the target, not the mid-transition blend). */
    val isDark: Boolean
        @Composable get() = LocalIsDark.current
}

private val GlassShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
)

// Tight tracking on large text and generous weights — the iOS feel.
private val GlassTypography = Typography(
    displayLarge = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp),
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontSize = 17.sp, letterSpacing = (-0.2).sp),
    bodyMedium = TextStyle(fontSize = 15.sp, letterSpacing = (-0.1).sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
)

@Composable
fun NovaTheme(
    mode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    // One float drives the whole palette, so a mode flip is a smooth dissolve.
    val t by animateFloatAsState(
        targetValue = if (dark) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "theme",
    )
    val glass = lerp(LightGlass, DarkGlass, t)

    // Material components (text fields, dialogs) read these; keep them in step.
    val scheme = lightColorScheme(
        primary = glass.ink,
        onPrimary = glass.onInk,
        secondary = NovaAccent,
        background = glass.canvasTop,
        onBackground = glass.textPrimary,
        surface = glass.canvasBottom,
        onSurface = glass.textPrimary,
        surfaceVariant = glass.glassFillStrong,
        onSurfaceVariant = glass.textSecondary,
        surfaceContainerHigh = lerp(Color.White, Color(0xFF15161A), t),
        outline = glass.glassEdgeTop,
        outlineVariant = glass.glassEdgeBottom,
        error = glass.danger,
    )

    CompositionLocalProvider(LocalGlass provides glass, LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = GlassShapes,
            typography = GlassTypography,
            content = content,
        )
    }
}
