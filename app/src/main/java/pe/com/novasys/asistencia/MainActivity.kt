package pe.com.novasys.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import pe.com.novasys.asistencia.ui.components.AuroraBackground
import pe.com.novasys.asistencia.ui.components.LocalHazeState
import pe.com.novasys.asistencia.ui.home.HomeScreen
import pe.com.novasys.asistencia.ui.intro.IntroOverlay
import pe.com.novasys.asistencia.ui.login.LoginScreen
import pe.com.novasys.asistencia.ui.theme.NovaTheme
import pe.com.novasys.asistencia.ui.theme.StatusBarIcons
import pe.com.novasys.asistencia.ui.theme.ThemeMode
import pe.com.novasys.asistencia.ui.update.UpdateBanner
import pe.com.novasys.asistencia.ui.update.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash shows the emblem; IntroOverlay picks it up from there.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NovasysApp).container
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle()
            NovaTheme(mode) {
                AppRoot(container)
            }
        }
    }
}

@Composable
private fun AppRoot(container: AppContainer) {
    val isDark = NovaTheme.isDark
    StatusBarIcons(darkIcons = !isDark)

    val email by container.auth.signedInEmail.collectAsStateWithLifecycle()
    val haze = remember { HazeState() }
    // Survives rotation so the intro only ever plays on a real cold start.
    var introDone by rememberSaveable { mutableStateOf(false) }
    val updates: UpdateViewModel = viewModel {
        UpdateViewModel(container.updates, enabled = BuildConfig.UPDATES_ENABLED)
    }

    Box(Modifier.fillMaxSize()) {
        // The one backdrop every glass surface blurs.
        AuroraBackground(Modifier.fillMaxSize().hazeSource(haze))

        CompositionLocalProvider(LocalHazeState provides haze) {
            // Routes on the session alone: signing out from anywhere lands on login.
            AnimatedContent(
                targetState = email,
                contentKey = { it != null },
                transitionSpec = {
                    (fadeIn(tween(420, delayMillis = 90)) +
                        scaleIn(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow), initialScale = 0.92f)) togetherWith
                        (fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 1.06f))
                },
                label = "session",
            ) { signedIn ->
                if (signedIn == null) {
                    LoginScreen(container)
                } else {
                    HomeScreen(
                        container = container,
                        sessionKey = signedIn,
                        isDark = isDark,
                        onToggleTheme = {
                            container.settings.setThemeMode(if (isDark) ThemeMode.Light else ThemeMode.Dark)
                        },
                    )
                }
            }

            UpdateBanner(updates, Modifier.align(Alignment.TopCenter))
        }

        if (!introDone) {
            IntroOverlay(onFinished = { introDone = true })
        }
    }
}
