package pe.com.novasys.asistencia.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import pe.com.novasys.asistencia.data.auth.AuthRepository
import pe.com.novasys.asistencia.data.auth.CognitoException
import java.io.IOException
import java.time.Instant

/**
 * Client for the asistencia-novasys-v2 REST API.
 *
 * Every call carries the Cognito ID token as `Authorization: Bearer`, which the
 * backend's `requireSession()` verifies (see src/lib/mobile-auth.ts). A 401 gets
 * exactly one retry with a freshly refreshed token before the session is
 * treated as gone.
 */
class NovaApi(
    baseUrl: String,
    private val http: OkHttpClient,
    private val auth: AuthRepository,
    private val json: Json,
) {
    private val baseUrl = baseUrl.trimEnd('/')

    suspend fun profile(): Profile =
        call("/api/profile") { json.decodeFromString<ProfileResponse>(it).employee }

    suspend fun today(): TodayStatus =
        call("/api/attendance/today") { json.decodeFromString<TodayStatus>(it) }

    suspend fun record(event: AttendanceEvent, deviceId: String) {
        val body = json.encodeToString(
            RecordEventRequest(
                eventType = event.wire,
                clientTime = Instant.now().toString(),
                deviceId = deviceId,
            )
        )
        call("/api/attendance/record", postJson = body) { }
    }

    private suspend fun <T> call(
        path: String,
        postJson: String? = null,
        parse: (String) -> T,
    ): T {
        val token = auth.idToken() ?: throw SessionExpiredException()
        var result = send(path, postJson, token)

        if (result.code == 401) {
            val refreshed = auth.idToken(forceRefresh = true) ?: throw SessionExpiredException()
            result = send(path, postJson, refreshed)
            if (result.code == 401) {
                // A token Cognito just issued still being refused means the
                // backend won't accept this user at all; stop retrying.
                auth.signOut()
                throw SessionExpiredException(errorMessage(result.body))
            }
        }

        if (result.code !in 200..299) {
            throw ApiException(result.code, errorMessage(result.body) ?: "Error del servidor (${result.code})")
        }
        return parse(result.body)
    }

    private suspend fun send(path: String, postJson: String?, token: String): RawResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .apply { if (postJson != null) post(postJson.toRequestBody(JSON)) }
                .build()
            http.newCall(request).execute().use { RawResponse(it.code, it.body.string()) }
        }

    private fun errorMessage(body: String): String? =
        runCatching { json.decodeFromString<ApiErrorBody>(body).error }.getOrNull()

    private data class RawResponse(val code: Int, val body: String)

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

class ApiException(val code: Int, message: String) : Exception(message)

class SessionExpiredException(message: String? = null) :
    Exception(message ?: "Tu sesión expiró. Vuelve a iniciar sesión.")

/** A message fit to show the user for any failure the data layer can raise. */
fun Throwable.userMessage(): String = when (this) {
    is CognitoException -> userMessage
    is ApiException, is SessionExpiredException -> message.orEmpty()
    is IOException -> "No hay conexión con el servidor. Revisa tu internet."
    else -> "Ocurrió un error inesperado. Intenta nuevamente."
}
