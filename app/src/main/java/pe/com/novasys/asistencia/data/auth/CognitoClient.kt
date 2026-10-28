package pe.com.novasys.asistencia.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Minimal Cognito Identity Provider client covering the two InitiateAuth flows
 * the app needs.
 *
 * Talks to the public JSON endpoint directly instead of pulling in the AWS SDK:
 * InitiateAuth against an app client without a secret needs no request signing,
 * and this keeps the APK small. It is the same USER_PASSWORD_AUTH flow the web
 * app's `cognitoSignIn` uses, against the same pool and client.
 */
class CognitoClient(
    private val http: OkHttpClient,
    private val json: Json,
    region: String,
    private val clientId: String,
) {
    private val endpoint = "https://cognito-idp.$region.amazonaws.com/"

    suspend fun signIn(email: String, password: String): CognitoTokens =
        initiateAuth(
            flow = "USER_PASSWORD_AUTH",
            parameters = mapOf("USERNAME" to email.trim().lowercase(), "PASSWORD" to password),
        )

    /** Cognito does not rotate the refresh token here, so the result carries none. */
    suspend fun refresh(refreshToken: String): CognitoTokens =
        initiateAuth(
            flow = "REFRESH_TOKEN_AUTH",
            parameters = mapOf("REFRESH_TOKEN" to refreshToken),
        )

    private suspend fun initiateAuth(
        flow: String,
        parameters: Map<String, String>,
    ): CognitoTokens = withContext(Dispatchers.IO) {
        val body = json.encodeToString(InitiateAuthRequest(flow, clientId, parameters))
        val request = Request.Builder()
            .url(endpoint)
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService.InitiateAuth")
            .post(body.toRequestBody(AMZ_JSON))
            .build()

        http.newCall(request).execute().use { response ->
            val text = response.body.string()
            if (!response.isSuccessful) {
                val error = runCatching { json.decodeFromString<CognitoErrorBody>(text) }.getOrNull()
                // `__type` sometimes arrives namespaced ("…#NotAuthorizedException").
                val type = error?.type?.substringAfterLast('#') ?: "HTTP_${response.code}"
                throw CognitoException(type, error?.message ?: error?.capitalMessage)
            }

            val parsed = json.decodeFromString<InitiateAuthResponse>(text)
            val result = parsed.authenticationResult
                // A challenge (e.g. NEW_PASSWORD_REQUIRED for invited users who
                // never set a password) has to be completed on the web for now.
                ?: throw CognitoException(parsed.challengeName ?: "UnknownChallenge", null)

            CognitoTokens(
                idToken = result.idToken,
                accessToken = result.accessToken,
                refreshToken = result.refreshToken,
                expiresInSeconds = result.expiresIn,
            )
        }
    }

    private companion object {
        val AMZ_JSON = "application/x-amz-json-1.1".toMediaType()
    }
}

data class CognitoTokens(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
)

class CognitoException(val type: String, detail: String?) : Exception("$type: ${detail.orEmpty()}") {

    /** Cognito said the credential itself is bad — not a network or server hiccup. */
    val isInvalidCredential: Boolean
        get() = type == "NotAuthorizedException" || type == "UserNotFoundException"

    /**
     * Same wording as the web's ERROR_MESSAGES. Wrong password and unknown email
     * share one message on purpose so the login can't be used to probe which
     * addresses have accounts.
     */
    val userMessage: String
        get() = when (type) {
            "NotAuthorizedException", "UserNotFoundException" -> "Correo o contraseña incorrectos"
            "UserNotConfirmedException" -> "Tu cuenta no ha sido verificada. Revisa tu correo."
            "PasswordResetRequiredException" -> "Debes restablecer tu contraseña desde la web."
            "NEW_PASSWORD_REQUIRED" -> "Primero define tu contraseña desde el enlace de invitación en la web."
            "LimitExceededException", "TooManyRequestsException" -> "Demasiados intentos, intenta más tarde"
            else -> "Error de autenticación. Intenta nuevamente."
        }
}

@Serializable
private data class InitiateAuthRequest(
    @SerialName("AuthFlow") val authFlow: String,
    @SerialName("ClientId") val clientId: String,
    @SerialName("AuthParameters") val authParameters: Map<String, String>,
)

@Serializable
private data class InitiateAuthResponse(
    @SerialName("AuthenticationResult") val authenticationResult: AuthenticationResult? = null,
    @SerialName("ChallengeName") val challengeName: String? = null,
)

@Serializable
private data class AuthenticationResult(
    @SerialName("IdToken") val idToken: String,
    @SerialName("AccessToken") val accessToken: String,
    @SerialName("RefreshToken") val refreshToken: String? = null,
    @SerialName("ExpiresIn") val expiresIn: Long,
)

@Serializable
private data class CognitoErrorBody(
    @SerialName("__type") val type: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("Message") val capitalMessage: String? = null,
)
