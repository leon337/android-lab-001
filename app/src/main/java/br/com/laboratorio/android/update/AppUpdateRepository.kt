package br.com.laboratorio.android.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class InstalledAppVersion(
    val versionName: String,
    val versionCode: Long,
    val signingCertificateSha256: String,
)

data class AvailableRelease(
    val tagName: String,
    val versionName: String,
    val versionCode: Long,
    val publishedAt: String,
    val notes: String,
    val apkAssetName: String,
    val apkUrl: String,
    val apkSize: Long,
    val manifestUrl: String,
    val manifestDigest: String,
)

private data class ReleaseManifest(
    val schemaVersion: Int,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val apkAsset: String,
    val apkSha256: String,
    val signingCertificateSha256: String,
)

class AppUpdateRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .build()

    fun installedVersion(): InstalledAppVersion {
        val info = packageManager.getPackageInfo(
            appContext.packageName,
            signingInfoFlags(),
        )
        return InstalledAppVersion(
            versionName = info.versionName ?: "desconhecida",
            versionCode = versionCode(info),
            signingCertificateSha256 = signingCertificateSha256(info),
        )
    }

    fun fetchReleases(): List<AvailableRelease> {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "GeminiLiveAndroid")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub não respondeu corretamente (HTTP ${response.code}).")
            }
            val raw = response.body?.string().orEmpty()
            return ReleaseCatalogParser.parse(raw)
        }
    }

    fun downloadAndVerify(
        release: AvailableRelease,
        onProgress: (Int) -> Unit,
    ): File {
        val installed = installedVersion()
        if (release.versionCode <= installed.versionCode) {
            throw IOException("Esta versão não é mais recente que a versão instalada. Downgrade não está habilitado.")
        }

        val manifest = fetchManifest(release)
        if (manifest.schemaVersion != MANIFEST_SCHEMA_VERSION) {
            throw IOException("O manifesto desta versão usa um formato não suportado.")
        }
        if (manifest.packageName != appContext.packageName) {
            throw IOException("O pacote do APK não corresponde a este aplicativo.")
        }
        if (manifest.versionName != release.versionName || manifest.versionCode != release.versionCode) {
            throw IOException("A versão declarada no manifesto não corresponde à release.")
        }
        if (manifest.apkAsset != release.apkAssetName) {
            throw IOException("O nome do APK não corresponde ao manifesto.")
        }
        if (manifest.minSdk < 26 || manifest.minSdk > Build.VERSION.SDK_INT) {
            throw IOException("A versão Android exigida pelo APK não é compatível com este dispositivo.")
        }

        val expectedSigner = normalizeSha256(manifest.signingCertificateSha256)
        if (expectedSigner != installed.signingCertificateSha256) {
            throw IOException(
                "A assinatura desta release é diferente da instalação atual. " +
                    "É necessária uma migração única de assinatura antes de atualizar.",
            )
        }

        if (release.apkSize > MAX_APK_BYTES) {
            throw IOException("O APK excede o limite de tamanho permitido.")
        }

        val updatesDirectory = File(appContext.filesDir, "updates")
        if (!updatesDirectory.isDirectory && !updatesDirectory.mkdirs()) {
            throw IOException("Não foi possível preparar o armazenamento privado da atualização.")
        }
        val destination = File(updatesDirectory, "${release.tagName}.apk".replace(Regex("[^A-Za-z0-9._-]"), "_"))
        val request = Request.Builder()
            .url(release.apkUrl)
            .header("User-Agent", "GeminiLiveAndroid")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Falha ao baixar o APK (HTTP ${response.code}).")
                }
                val body = response.body ?: throw IOException("O GitHub retornou um APK vazio.")
                val totalBytes = body.contentLength().takeIf { it > 0L } ?: release.apkSize
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                body.byteStream().use { input ->
                    destination.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            copied += count
                            if (copied > MAX_APK_BYTES) throw IOException("Download excedeu o limite permitido.")
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                            if (totalBytes > 0L) onProgress(((copied * 100) / totalBytes).toInt().coerceIn(0, 99))
                        }
                    }
                }
                val actualDigest = digest.digest().toHex()
                if (!actualDigest.equals(normalizeSha256(manifest.apkSha256), ignoreCase = true)) {
                    throw IOException("O SHA-256 do APK não confere. O arquivo foi descartado.")
                }
            }
        } catch (error: Exception) {
            destination.delete()
            throw error
        }

        try {
            verifyDownloadedPackage(destination, manifest, expectedSigner)
        } catch (error: Exception) {
            destination.delete()
            throw error
        }

        onProgress(100)
        return destination
    }

    private fun fetchManifest(release: AvailableRelease): ReleaseManifest {
        val request = Request.Builder()
            .url(release.manifestUrl)
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "GeminiLiveAndroid")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Não foi possível obter o manifesto da release (HTTP ${response.code}).")
            }
            val body = response.body ?: throw IOException("Manifesto vazio.")
            val bytes = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(4_096)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_MANIFEST_BYTES) {
                        throw IOException("Manifesto excede o tamanho permitido.")
                    }
                    bytes.write(buffer, 0, count)
                }
            }
            val manifestBytes = bytes.toByteArray()
            val actualDigest = MessageDigest.getInstance("SHA-256").digest(manifestBytes).toHex()
            if (!actualDigest.equals(normalizeSha256(release.manifestDigest), ignoreCase = true)) {
                throw IOException("A integridade do manifesto não foi confirmada.")
            }
            val json = JSONObject(manifestBytes.toString(Charsets.UTF_8))
            return ReleaseManifest(
                schemaVersion = json.optInt("schemaVersion", 0),
                packageName = json.optString("packageName"),
                versionName = json.optString("versionName"),
                versionCode = json.optLong("versionCode", -1L),
                minSdk = json.optInt("minSdk", 0),
                apkAsset = json.optString("apkAsset"),
                apkSha256 = json.optString("apkSha256"),
                signingCertificateSha256 = json.optString("signingCertificateSha256"),
            )
        }
    }

    private fun verifyDownloadedPackage(
        apk: File,
        manifest: ReleaseManifest,
        expectedSigner: String,
    ) {
        val info = packageManager.getPackageArchiveInfo(apk.absolutePath, signingInfoFlags())
            ?: throw IOException("O arquivo baixado não é um APK Android válido.")
        if (info.packageName != appContext.packageName) {
            throw IOException("O APK baixado pertence a outro aplicativo.")
        }
        if (info.versionName != manifest.versionName || versionCode(info) != manifest.versionCode) {
            throw IOException("A versão interna do APK não corresponde ao manifesto.")
        }
        val archiveSigner = signingCertificateSha256(info)
        if (archiveSigner != expectedSigner) {
            throw IOException("A assinatura criptográfica do APK não corresponde ao manifesto.")
        }
    }

    @Suppress("DEPRECATION")
    private fun signingInfoFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    private fun signingCertificateSha256(info: PackageInfo): String {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners
        } else {
            info.signatures
        }
        val signature = signatures?.singleOrNull()
            ?: throw IOException("Não foi possível identificar uma assinatura única do APK.")
        return MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).toHex()
    }

    private fun normalizeSha256(value: String): String =
        value.removePrefix("sha256:").replace(":", "").trim().uppercase()

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02X".format(it) }

    private companion object {
        const val RELEASES_URL =
            "https://api.github.com/repos/leon337/android-lab-001/releases?per_page=30"
        const val MANIFEST_SCHEMA_VERSION = 1
        const val MAX_APK_BYTES = 200L * 1024L * 1024L
        const val MAX_MANIFEST_BYTES = 64 * 1024
    }
}
