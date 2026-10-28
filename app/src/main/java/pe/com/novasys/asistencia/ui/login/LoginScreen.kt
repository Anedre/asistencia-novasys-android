package pe.com.novasys.asistencia.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.com.novasys.asistencia.AppContainer
import pe.com.novasys.asistencia.BuildConfig
import pe.com.novasys.asistencia.ui.components.GlassButton
import pe.com.novasys.asistencia.ui.components.GlassSurface
import pe.com.novasys.asistencia.ui.components.OrbitingEmblem
import pe.com.novasys.asistencia.ui.components.staggerIn
import pe.com.novasys.asistencia.ui.theme.NovaAccent
import pe.com.novasys.asistencia.ui.theme.NovaTheme

@Composable
fun LoginScreen(container: AppContainer) {
    val vm: LoginViewModel = viewModel { LoginViewModel(container.auth) }
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val p = NovaTheme.glass

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(56.dp))

        OrbitingEmblem(
            size = 104.dp,
            ringColor = p.textPrimary.copy(alpha = 0.5f),
            accent = NovaAccent,
            starColor = p.textPrimary,
            modifier = Modifier.staggerIn(0),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "NOVA",
            color = p.textPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            modifier = Modifier.staggerIn(1),
        )
        Text(
            "ASSISTANCE",
            color = p.accentText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 5.sp,
            modifier = Modifier.staggerIn(1),
        )

        Spacer(Modifier.height(40.dp))

        GlassSurface(
            strong = true,
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .staggerIn(2),
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Inicia sesión", color = p.textPrimary, style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Usa el mismo correo y contraseña que en la web.",
                    color = p.textSecondary,
                    fontSize = 15.sp,
                )

                GlassField(
                    value = state.email,
                    onValueChange = vm::onEmailChange,
                    label = "Correo",
                    enabled = !state.loading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                )

                GlassField(
                    value = state.password,
                    onValueChange = vm::onPasswordChange,
                    label = "Contraseña",
                    enabled = !state.loading,
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        TextButton(onClick = vm::togglePasswordVisible) {
                            Text(if (state.passwordVisible) "Ocultar" else "Ver", color = p.accentText)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus()
                        vm.signIn()
                    }),
                )

                AnimatedVisibility(
                    visible = state.error != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    GlassSurface(
                        tint = p.danger.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            state.error.orEmpty(),
                            color = p.danger,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }

                GlassButton(
                    text = "Ingresar",
                    onClick = {
                        focus.clearFocus()
                        vm.signIn()
                    },
                    enabled = state.canSubmit,
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Password recovery stays on the web, which already owns that flow.
        TextButton(
            onClick = { uriHandler.openUri("${BuildConfig.API_BASE_URL}/forgot-password") },
            modifier = Modifier
                .padding(top = 12.dp)
                .staggerIn(3),
        ) {
            Text("¿Olvidaste tu contraseña?", color = p.textSecondary)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GlassField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = NovaTheme.glass
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        trailingIcon = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = p.glassFill,
            unfocusedContainerColor = p.glassFill,
            disabledContainerColor = p.glassFill,
            focusedBorderColor = NovaAccent,
            unfocusedBorderColor = p.textPrimary.copy(alpha = 0.12f),
            disabledBorderColor = p.textPrimary.copy(alpha = 0.06f),
            focusedLabelColor = p.accentText,
            unfocusedLabelColor = p.textMuted,
            focusedTextColor = p.textPrimary,
            unfocusedTextColor = p.textPrimary,
            disabledTextColor = p.textSecondary,
            cursorColor = NovaAccent,
            errorBorderColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
