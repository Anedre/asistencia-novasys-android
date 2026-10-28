package pe.com.novasys.asistencia.ui.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.com.novasys.asistencia.ui.components.GlassButton
import pe.com.novasys.asistencia.ui.components.GlassButtonStyle
import pe.com.novasys.asistencia.ui.components.GlassProgress
import pe.com.novasys.asistencia.ui.components.GlassSurface
import pe.com.novasys.asistencia.ui.theme.NovaAccent
import pe.com.novasys.asistencia.ui.theme.NovaTheme

/** Floating glass card that drops in from the top when a new version exists. */
@Composable
fun UpdateBanner(vm: UpdateViewModel, modifier: Modifier = Modifier) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(vm) {
        vm.onResume(context)
        onPauseOrDispose { }
    }

    // Hold on to the last visible state so the exit animation still has
    // something to draw after the state has already gone back to Idle.
    val visible = state !is UpdateState.Idle
    val holder = remember { arrayOfNulls<UpdateState>(1) }
    if (visible) holder[0] = state
    val lastShown = holder[0]

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) { -it } +
            fadeIn(tween(250)),
        exit = slideOutVertically(tween(260)) { -it } + fadeOut(tween(200)),
        modifier = modifier
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        val shown = lastShown ?: return@AnimatedVisibility
        GlassSurface(modifier = Modifier.fillMaxWidth(), strong = true) {
            AnimatedContent(
                targetState = shown,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "update-content",
            ) { s -> BannerContent(s, vm) }
        }
    }
}

@Composable
private fun BannerContent(state: UpdateState, vm: UpdateViewModel) {
    val p = NovaTheme.glass
    val context = LocalContext.current

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state) {
            is UpdateState.Available -> {
                Title("Nueva versión ${state.update.version}")
                if (state.update.notes.isNotBlank()) {
                    Text(
                        state.update.notes,
                        color = p.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Actions(
                    primary = "Actualizar" to { vm.download(context, state.update) },
                    secondary = "Luego" to vm::dismiss,
                )
            }
            is UpdateState.Downloading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Title("Descargando ${state.update.version}", Modifier.weight(1f))
                    Text("${(state.progress * 100).toInt()}%", color = p.textSecondary, fontSize = 14.sp)
                }
                GlassProgress(state.progress, NovaAccent)
            }
            is UpdateState.NeedsPermission -> {
                Title("Permite la instalación")
                Text(
                    "Android pide autorizar a Nova Asistencia para instalar su propia actualización. " +
                        "Actívalo y vuelve: la instalación sigue sola.",
                    color = p.textSecondary,
                    fontSize = 14.sp,
                )
                Actions(
                    primary = "Abrir ajustes" to { vm.openInstallPermissionSettings(context) },
                    secondary = "Luego" to vm::dismiss,
                )
            }
            is UpdateState.Failed -> {
                Title(state.message)
                Actions(
                    primary = "Reintentar" to { vm.download(context, state.update) },
                    secondary = "Cerrar" to vm::dismiss,
                )
            }
            UpdateState.Idle -> Unit
        }
    }
}

@Composable
private fun Title(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = NovaTheme.glass.textPrimary,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    )
}

@Composable
private fun Actions(primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
            text = secondary.first,
            onClick = secondary.second,
            style = GlassButtonStyle.Glass,
            height = 44.dp,
            modifier = Modifier.weight(1f),
        )
        GlassButton(
            text = primary.first,
            onClick = primary.second,
            height = 44.dp,
            modifier = Modifier.weight(1f),
        )
    }
}
