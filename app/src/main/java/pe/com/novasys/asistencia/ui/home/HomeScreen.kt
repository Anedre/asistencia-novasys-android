package pe.com.novasys.asistencia.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import pe.com.novasys.asistencia.AppContainer
import pe.com.novasys.asistencia.data.api.AttendanceEvent
import pe.com.novasys.asistencia.data.api.ShiftPhase
import pe.com.novasys.asistencia.data.api.TodayStatus
import pe.com.novasys.asistencia.ui.components.GlassButton
import pe.com.novasys.asistencia.ui.components.GlassButtonStyle
import pe.com.novasys.asistencia.ui.components.GlassIconButton
import pe.com.novasys.asistencia.ui.components.GlassPill
import pe.com.novasys.asistencia.ui.components.GlassProgress
import pe.com.novasys.asistencia.ui.components.GlassSurface
import pe.com.novasys.asistencia.ui.components.RollingText
import pe.com.novasys.asistencia.ui.components.SignOutGlyph
import pe.com.novasys.asistencia.ui.components.SunMoonGlyph
import pe.com.novasys.asistencia.ui.components.shimmer
import pe.com.novasys.asistencia.ui.components.staggerIn
import pe.com.novasys.asistencia.ui.theme.NovaAccent
import pe.com.novasys.asistencia.ui.theme.NovaTheme
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The backend closes work days in Lima time (workDateLima), so the app does too. */
private val LIMA = ZoneId.of("America/Lima")
private val SPANISH = Locale.forLanguageTag("es-PE")
private val HOURS_MINUTES = DateTimeFormatter.ofPattern("HH:mm", SPANISH)
private val SECONDS = DateTimeFormatter.ofPattern("ss", SPANISH)
// Lowercase weekday and month, as the web writes dates ("martes, 27 de octubre").
private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SPANISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    sessionKey: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
) {
    val vm: HomeViewModel = viewModel(key = "home-$sessionKey") {
        HomeViewModel(container.api, container.auth, container.tokenStore.deviceId())
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    var confirmEnd by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.messageShown()
        }
    }
    LaunchedEffect(state.successCount) {
        if (state.successCount > 0) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.loading && state.today != null,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Header(
                    name = state.profile?.displayFirstName,
                    subtitle = listOfNotNull(state.profile?.position, state.profile?.area)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    isAdmin = state.profile?.isAdmin == true,
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    onSignOut = vm::signOut,
                    modifier = Modifier.staggerIn(0),
                )

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ErrorBanner(state.error.orEmpty(), onRetry = vm::refresh)
                }

                val today = state.today
                if (today == null) {
                    if (state.loading) Skeleton()
                } else {
                    Hero(today, successCount = state.successCount, modifier = Modifier.staggerIn(1))
                    StatsGrid(today, modifier = Modifier.staggerIn(2))
                    Actions(
                        phase = today.phase,
                        submitting = state.submitting,
                        onRecord = { event ->
                            if (event == AttendanceEvent.End) confirmEnd = true else vm.record(event)
                        },
                        modifier = Modifier.staggerIn(3),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        ) { data ->
            GlassSurface(strong = true, shape = CircleShape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Box(Modifier.size(8.dp).background(NovaTheme.glass.success, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text(data.visuals.message, color = NovaTheme.glass.textPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // Ending the shift is the one action that can't be taken back from the
    // phone, so it asks first.
    if (confirmEnd) {
        val p = NovaTheme.glass
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("¿Marcar salida?", color = p.textPrimary) },
            text = { Text("Se cerrará tu jornada de hoy.", color = p.textSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    vm.record(AttendanceEvent.End)
                }) { Text("Marcar salida", color = p.accentText, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Cancelar", color = p.textSecondary) }
            },
        )
    }
}

@Composable
private fun Header(
    name: String?,
    subtitle: String,
    isAdmin: Boolean,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = NovaTheme.glass
    val hour = ZonedDateTime.now(LIMA).hour
    val greeting = when {
        hour < 12 -> "Buenos días"
        hour < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.padding(top = 8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(greeting, color = p.textSecondary, fontSize = 15.sp)
            Text(
                name ?: " ",
                color = p.textPrimary,
                style = MaterialTheme.typography.headlineLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = p.textMuted, fontSize = 13.sp)
                }
                if (isAdmin) {
                    if (subtitle.isNotBlank()) Spacer(Modifier.width(8.dp))
                    GlassPill("Admin", p.accentText)
                }
            }
        }
        GlassIconButton(onClick = onToggleTheme, contentDescription = "Cambiar tema") {
            SunMoonGlyph(dark = isDark, color = p.textPrimary)
        }
        Spacer(Modifier.width(10.dp))
        GlassIconButton(onClick = onSignOut, contentDescription = "Cerrar sesión") {
            SignOutGlyph(color = p.textPrimary)
        }
    }
}

@Composable
private fun Hero(today: TodayStatus, successCount: Int, modifier: Modifier = Modifier) {
    val p = NovaTheme.glass
    var now by remember { mutableStateOf(ZonedDateTime.now(LIMA)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now(LIMA)
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    // A ring of light rolls out from the status pill each time an event lands.
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(successCount) {
        if (successCount > 0) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    val (label, tone) = when (today.phase) {
        ShiftPhase.NotStarted -> "Sin marcar" to p.warn
        ShiftPhase.Working -> "Trabajando" to p.success
        ShiftPhase.OnBreak -> "En refrigerio" to p.accentText
        ShiftPhase.Finished -> "Jornada cerrada" to p.textSecondary
    }

    GlassSurface(
        strong = true,
        shape = RoundedCornerShape(32.dp),
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                val v = pulse.value
                if (v < 1f) {
                    drawCircle(
                        color = tone.copy(alpha = (1f - v) * 0.55f),
                        radius = size.maxDimension * v,
                        center = Offset(size.width * 0.18f, size.height * 0.16f),
                        style = Stroke(width = 3.dp.toPx() * (1f - v) + 1f),
                    )
                }
            },
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = label to tone,
                    transitionSpec = {
                        (fadeIn(tween(260)) + scaleIn(initialScale = 0.85f)) togetherWith
                            (fadeOut(tween(180)) + scaleOut(targetScale = 1.1f))
                    },
                    label = "phase",
                ) { (text, color) ->
                    GlassPill(text, color, live = today.phase == ShiftPhase.Working)
                }
                if (today.isHoliday) {
                    Spacer(Modifier.width(8.dp))
                    GlassPill(today.holidayName?.let { "Feriado · $it" } ?: "Feriado", p.warn)
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                RollingText(
                    text = now.format(HOURS_MINUTES),
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Light),
                    color = p.textPrimary,
                )
                Spacer(Modifier.width(6.dp))
                RollingText(
                    text = now.format(SECONDS),
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
                    color = p.textMuted,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Text(now.format(LONG_DATE), color = p.textSecondary, fontSize = 16.sp)

            Spacer(Modifier.height(20.dp))
            val planned = today.plannedMinutes
            val progress = if (planned > 0) (today.workedMinutes / planned).toFloat() else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Jornada", color = p.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    if (planned > 0) "${today.workedHHMM} de ${minutesToHHMM(planned)}" else today.workedHHMM,
                    color = p.textSecondary,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            GlassProgress(progress, if (progress >= 1f) p.success else NovaAccent)
        }
    }
}

@Composable
private fun StatsGrid(today: TodayStatus, modifier: Modifier = Modifier) {
    val p = NovaTheme.glass
    val balanceColor = when {
        today.deltaMinutes > 0 -> p.success
        today.deltaMinutes < 0 -> p.danger
        else -> p.textPrimary
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Entrada", today.firstInLocal ?: "--:--", Modifier.weight(1f))
            Stat("Salida", today.lastOutLocal ?: "--:--", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Trabajado", today.workedHHMM, Modifier.weight(1f))
            Stat("Balance", today.deltaHHMM, Modifier.weight(1f), balanceColor)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    val p = NovaTheme.glass
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(label.uppercase(SPANISH), color = p.textMuted, fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            RollingText(
                text = value,
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
                color = valueColor ?: p.textPrimary,
            )
        }
    }
}

@Composable
private fun Actions(
    phase: ShiftPhase,
    submitting: AttendanceEvent?,
    onRecord: (AttendanceEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val busy = submitting != null
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn(tween(300, delayMillis = 80)) +
                slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)) { it / 3 }) togetherWith
                fadeOut(tween(160))
        },
        label = "actions",
        modifier = modifier,
    ) { current ->
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (current) {
                ShiftPhase.NotStarted -> GlassButton(
                    "Marcar entrada", { onRecord(AttendanceEvent.Start) },
                    Modifier.fillMaxWidth(), enabled = !busy, loading = submitting == AttendanceEvent.Start,
                    height = 60.dp,
                )
                ShiftPhase.Working -> {
                    GlassButton(
                        "Iniciar refrigerio", { onRecord(AttendanceEvent.BreakStart) },
                        Modifier.fillMaxWidth(), style = GlassButtonStyle.Glass,
                        enabled = !busy, loading = submitting == AttendanceEvent.BreakStart,
                    )
                    GlassButton(
                        "Marcar salida", { onRecord(AttendanceEvent.End) },
                        Modifier.fillMaxWidth(), enabled = !busy, loading = submitting == AttendanceEvent.End,
                        height = 60.dp,
                    )
                }
                ShiftPhase.OnBreak -> GlassButton(
                    "Terminar refrigerio", { onRecord(AttendanceEvent.BreakEnd) },
                    Modifier.fillMaxWidth(), enabled = !busy, loading = submitting == AttendanceEvent.BreakEnd,
                    height = 60.dp,
                )
                ShiftPhase.Finished -> GlassSurface(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tu jornada de hoy ya está cerrada. Si necesitas corregirla, solicita una regularización.",
                        color = NovaTheme.glass.textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
        }
    }
}

/** Glass placeholders in the real layout's shape, shimmering while data loads. */
@Composable
private fun Skeleton() {
    val p = NovaTheme.glass
    val block = p.textPrimary.copy(alpha = 0.07f)
    val highlight = p.specular.copy(alpha = 0.9f)

    @Composable
    fun Bar(widthFraction: Float, height: Int) {
        Box(
            Modifier
                .fillMaxWidth(widthFraction)
                .height(height.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(block)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.staggerIn(1)) {
        GlassSurface(strong = true, shape = RoundedCornerShape(32.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp).shimmer(highlight), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Bar(0.32f, 26)
                Bar(0.62f, 64)
                Bar(0.48f, 18)
                Spacer(Modifier.height(4.dp))
                Bar(1f, 8)
            }
        }
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) {
                    GlassSurface(shape = RoundedCornerShape(24.dp), modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(16.dp).shimmer(highlight), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Bar(0.5f, 12)
                            Bar(0.75f, 26)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    val p = NovaTheme.glass
    GlassSurface(
        tint = p.danger.copy(alpha = 0.12f),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                message,
                color = p.danger,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
            )
            TextButton(onClick = onRetry) { Text("Reintentar", color = p.danger, fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun minutesToHHMM(minutes: Double): String {
    val total = minutes.toInt()
    return "%02d:%02d".format(total / 60, total % 60)
}
