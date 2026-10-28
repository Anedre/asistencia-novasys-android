package pe.com.novasys.asistencia.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** A newer build published as a GitHub Release. */
data class AvailableUpdate(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
)

/**
 * Self-update channel: the app is distributed outside the Play Store for now,
 * so it checks the latest GitHub Release of its own repository and installs
 * the APK attached to it.
 *
 * Trust comes from Android itself, not from this code: the system refuses to
 * install an update unless it is signed with the same key as the installed
 * app, so a tampered or substituted APK cannot replace this one.
 */
class UpdateRepository(
    private val http: OkHttpClient,
    private val json: Json,
    private val repo: String,
    private val currentVersion: String,
    private val downloadDir: File,
) {
    /** The latest release if it is newer than this build, else null. */
    suspend fun latest(): AvailableUpdate? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .build()

        http.newCall(request).execute().use { response ->
            // 404 simply means nothing has been released yet.
            if (response.code == 404) return@withContext null
            if (!response.isSuccessful) throw IOException("GitHub respondió ${response.code}")

            val release = json.decodeFromString<GithubRelease>(response.body.string())
            val version = release.tagName.removePrefix("v")
            if (compareVersions(version, currentVersion) <= 0) return@withContext null

            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@withContext null

            AvailableUpdate(
                version = version,
                notes = release.body.orEmpty().trim(),
                apkUrl = apk.downloadUrl,
                apkSizeBytes = apk.size,
            )
        }
    }

    /** Download the APK into the app's cache, reporting progress 0..1. */
    suspend fun download(update: AvailableUpdate, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            downloadDir.mkdirs()
            // Only ever keep the update being installed.
            downloadDir.listFiles()?.forEach { it.delete() }
            val target = File(downloadDir, "nova-asistencia-${update.version}.apk")

            val request = Request.Builder().url(update.apkUrl).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Descarga falló (${response.code})")
                val body = response.body
                val total = body.contentLength().takeIf { it > 0 } ?: update.apkSizeBytes

                var written = 0L
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }

                if (update.apkSizeBytes > 0 && written != update.apkSizeBytes) {
                    target.delete()
                    throw IOException("La descarga llegó incompleta")
                }
            }
            target
        }

    companion object {
        /**
         * Compare dotted numeric versions ("0.10.2" > "0.9.9"). Anything after
         * a `-` or `+` is ignored, as are missing components (treated as 0).
         */
        fun compareVersions(a: String, b: String): Int {
            fun parts(v: String) = v.substringBefore('-').substringBefore('+')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }
            val pa = parts(a)
            val pb = parts(b)
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val diff = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
                if (diff != 0) return diff
            }
            return 0
        }
    }
}

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String,
    val size: Long = 0,
    @SerialName("browser_download_url") val downloadUrl: String,
)
