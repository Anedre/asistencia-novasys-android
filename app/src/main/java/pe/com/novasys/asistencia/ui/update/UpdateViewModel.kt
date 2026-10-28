package pe.com.novasys.asistencia.ui.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.com.novasys.asistencia.data.update.AvailableUpdate
import pe.com.novasys.asistencia.data.update.UpdateRepository
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val update: AvailableUpdate) : UpdateState
    data class Downloading(val update: AvailableUpdate, val progress: Float) : UpdateState
    /** Downloaded, waiting for the user to allow installs from this app. */
    data class NeedsPermission(val update: AvailableUpdate, val apk: File) : UpdateState
    data class Failed(val update: AvailableUpdate, val message: String) : UpdateState
}

class UpdateViewModel(
    private val updates: UpdateRepository,
    enabled: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    init {
        if (enabled) check()
    }

    private fun check() {
        viewModelScope.launch {
            try {
                updates.latest()?.let { _state.value = UpdateState.Available(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // A failed check is silent: no network or a GitHub hiccup should
                // never get in the way of marking attendance.
            }
        }
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    fun download(context: Context, update: AvailableUpdate) {
        val appContext = context.applicationContext
        _state.value = UpdateState.Downloading(update, 0f)
        viewModelScope.launch {
            try {
                val apk = updates.download(update) { progress ->
                    _state.value = UpdateState.Downloading(update, progress)
                }
                install(appContext, update, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = UpdateState.Failed(update, "No se pudo descargar la actualización.")
            }
        }
    }

    /** Called when the app comes back to the foreground (e.g. from Settings). */
    fun onResume(context: Context) {
        val pending = _state.value as? UpdateState.NeedsPermission ?: return
        if (context.packageManager.canRequestPackageInstalls()) {
            install(context.applicationContext, pending.update, pending.apk)
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun install(context: Context, update: AvailableUpdate, apk: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission(update, apk)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        // The system installer takes it from here; if the user backs out the
        // banner simply goes away until the next launch.
        _state.value = UpdateState.Idle
    }
}
