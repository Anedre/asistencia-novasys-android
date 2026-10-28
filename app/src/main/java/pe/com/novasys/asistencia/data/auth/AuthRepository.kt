package pe.com.novasys.asistencia.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the signed-in session: signing in, keeping the ID token fresh, and
 * signing out.
 *
 * All token reads and refreshes go through one mutex so that several requests
 * failing at once trigger a single refresh, not a burst of them.
 */
class AuthRepository(
    private val cognito: CognitoClient,
    private val store: TokenStore,
) {
    private val mutex = Mutex()
    private var session: StoredSession? = store.load()

    private val _signedInEmail = MutableStateFlow(session?.email)

    /** The signed-in user's email, or null when signed out. The UI keys off this. */
    val signedInEmail: StateFlow<String?> = _signedInEmail.asStateFlow()

    suspend fun signIn(email: String, password: String) {
        val tokens = cognito.signIn(email, password)
        val refreshToken = tokens.refreshToken
            ?: throw CognitoException("MissingRefreshToken", "Cognito no devolvió refresh token")
        mutex.withLock {
            persist(
                StoredSession(
                    idToken = tokens.idToken,
                    accessToken = tokens.accessToken,
                    refreshToken = refreshToken,
                    expiresAtEpochSeconds = nowSeconds() + tokens.expiresInSeconds,
                    email = email.trim().lowercase(),
                )
            )
        }
    }

    suspend fun signOut() {
        mutex.withLock { clearLocked() }
    }

    /**
     * A usable ID token, refreshed first if it is about to expire (or when the
     * server has just rejected it and [forceRefresh] is set).
     *
     * Returns null once the session is gone. Only a refresh token Cognito
     * rejects ends the session; a network failure is rethrown so a flaky
     * connection never logs the user out.
     */
    suspend fun idToken(forceRefresh: Boolean = false): String? = mutex.withLock {
        val current = session ?: return null
        val now = nowSeconds()
        if (!forceRefresh && current.expiresAtEpochSeconds - now > REFRESH_MARGIN_SECONDS) {
            return current.idToken
        }

        try {
            val tokens = cognito.refresh(current.refreshToken)
            val updated = current.copy(
                idToken = tokens.idToken,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken ?: current.refreshToken,
                expiresAtEpochSeconds = now + tokens.expiresInSeconds,
            )
            persist(updated)
            updated.idToken
        } catch (e: CognitoException) {
            if (!e.isInvalidCredential) throw e
            clearLocked()
            null
        }
    }

    private fun persist(updated: StoredSession) {
        session = updated
        store.save(updated)
        _signedInEmail.value = updated.email
    }

    private fun clearLocked() {
        session = null
        store.clear()
        _signedInEmail.value = null
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private companion object {
        /** Refresh a little early so a request never leaves with a token about to lapse. */
        const val REFRESH_MARGIN_SECONDS = 120L
    }
}
