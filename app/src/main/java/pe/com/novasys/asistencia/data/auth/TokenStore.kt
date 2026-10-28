package pe.com.novasys.asistencia.data.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class StoredSession(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
    val email: String,
)

/**
 * Persists the Cognito session encrypted with an AES-GCM key that lives in the
 * Android Keystore and never leaves it.
 *
 * The refresh token is good for 30 days, so it gets the same care as a
 * password. Backups are disabled in the manifest; if the blob ever can't be
 * decrypted (key wiped, restored onto another device) the session is simply
 * dropped and the user signs in again.
 */
class TokenStore(context: Context, private val json: Json) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): StoredSession? {
        val blob = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            json.decodeFromString<StoredSession>(decrypt(blob))
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun save(session: StoredSession) {
        prefs.edit { putString(KEY_SESSION, encrypt(json.encodeToString(session))) }
    }

    fun clear() {
        prefs.edit { remove(KEY_SESSION) }
    }

    /**
     * A random per-install id sent with each attendance event, the native
     * counterpart of the web's deviceId. Deliberately not ANDROID_ID: it only
     * needs to tell this install apart, not identify the phone.
     */
    fun deviceId(): String =
        prefs.getString(KEY_DEVICE_ID, null)
            ?: "android-${UUID.randomUUID()}".also { id ->
                prefs.edit { putString(KEY_DEVICE_ID, id) }
            }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val sealed = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val sealed = Base64.decode(blob, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_BITS, sealed, 0, GCM_IV_BYTES),
        )
        val plain = cipher.doFinal(sealed, GCM_IV_BYTES, sealed.size - GCM_IV_BYTES)
        return String(plain, Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS_NAME = "nova_session"
        const val KEY_SESSION = "session"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_ALIAS = "nova_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
