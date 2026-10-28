package pe.com.novasys.asistencia

import android.app.Application
import android.content.Context
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import pe.com.novasys.asistencia.data.api.NovaApi
import pe.com.novasys.asistencia.data.auth.AuthRepository
import pe.com.novasys.asistencia.data.auth.CognitoClient
import pe.com.novasys.asistencia.data.auth.TokenStore
import pe.com.novasys.asistencia.data.settings.SettingsStore
import pe.com.novasys.asistencia.data.update.UpdateRepository
import java.io.File
import java.util.concurrent.TimeUnit

class NovasysApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Hand-wired dependencies, one instance each for the whole process.
 *
 * Small enough that a DI framework would cost more than it saves; swap in Hilt
 * once the admin screens multiply the number of repositories.
 */
class AppContainer(context: Context) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val tokenStore = TokenStore(context, json)

    val auth = AuthRepository(
        cognito = CognitoClient(http, json, BuildConfig.COGNITO_REGION, BuildConfig.COGNITO_CLIENT_ID),
        store = tokenStore,
    )

    val api = NovaApi(BuildConfig.API_BASE_URL, http, auth, json)

    val settings = SettingsStore(context)

    val updates = UpdateRepository(
        http = http,
        json = json,
        repo = BuildConfig.UPDATE_REPO,
        currentVersion = BuildConfig.VERSION_NAME,
        downloadDir = File(context.cacheDir, "updates"),
    )
}
