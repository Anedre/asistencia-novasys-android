package pe.com.novasys.asistencia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.com.novasys.asistencia.data.api.AttendanceEvent
import pe.com.novasys.asistencia.data.api.NovaApi
import pe.com.novasys.asistencia.data.api.Profile
import pe.com.novasys.asistencia.data.api.SessionExpiredException
import pe.com.novasys.asistencia.data.api.TodayStatus
import pe.com.novasys.asistencia.data.api.userMessage
import pe.com.novasys.asistencia.data.auth.AuthRepository

data class HomeUiState(
    val loading: Boolean = true,
    val profile: Profile? = null,
    val today: TodayStatus? = null,
    val error: String? = null,
    /** The event being sent right now, so only its button shows progress. */
    val submitting: AttendanceEvent? = null,
    /** One-shot confirmation for the snackbar; cleared once shown. */
    val message: String? = null,
    /** Bumped on every recorded event; the UI plays its success pulse on change. */
    val successCount: Int = 0,
)

class HomeViewModel(
    private val api: NovaApi,
    private val auth: AuthRepository,
    private val deviceId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatchingUi {
                coroutineScope {
                    val profile = async { api.profile() }
                    val today = async { api.today() }
                    _state.update {
                        it.copy(loading = false, profile = profile.await(), today = today.await())
                    }
                }
            }
        }
    }

    fun record(event: AttendanceEvent) {
        if (_state.value.submitting != null) return
        _state.update { it.copy(submitting = event, error = null) }

        viewModelScope.launch {
            runCatchingUi {
                api.record(event, deviceId)
                // Re-read rather than patching locally: the server owns the
                // worked/late/balance math and may have closed or adjusted the shift.
                val today = api.today()
                _state.update {
                    it.copy(
                        submitting = null,
                        today = today,
                        message = confirmationFor(event),
                        successCount = it.successCount + 1,
                    )
                }
            }
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    private suspend fun runCatchingUi(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: SessionExpiredException) {
            // AuthRepository has already cleared the session; the root
            // composable routes back to login on its own.
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, submitting = null, error = e.userMessage()) }
        }
    }

    private fun confirmationFor(event: AttendanceEvent): String = when (event) {
        AttendanceEvent.Start -> "Entrada registrada"
        AttendanceEvent.BreakStart -> "Refrigerio iniciado"
        AttendanceEvent.BreakEnd -> "Refrigerio terminado"
        AttendanceEvent.End -> "Salida registrada. ¡Buen descanso!"
    }
}
