package br.com.laboratorio.android.update

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import okio.Buffer
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class AppUpdateRepositoryInstrumentedTest {
    @Test
    fun baixaEValidaApkAssinadoAntesDePromoverArquivoFinal() {
        val scenario = createScenario()
        try {
            val progress = mutableListOf<Int>()
            val downloaded = scenario.repository.downloadAndVerify(scenario.release) { progress.add(it) }

            assertTrue(downloaded.isFile)
            assertEquals(scenario.apkBytes.size.toLong(), downloaded.length())
            assertEquals(100, progress.last())
            assertFalse(scenario.updatesDirectory.listFiles().orEmpty().any { it.name.endsWith(".part") })
            downloaded.delete()
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaManifestoComDigestIncorreto() {
        val scenario = createScenario(manifestDigestOverride = sha256("manifesto adulterado".toByteArray()))
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertEquals(1, scenario.server.requestCount)
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaApkComSha256IncorretoERemoveArquivoTemporario() {
        val scenario = createScenario { manifest -> manifest.put("apkSha256", "0".repeat(64)) }
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertFalse(scenario.updatesDirectory.listFiles().orEmpty().any { it.name.endsWith(".part") })
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaDownloadComTamanhoDiferenteDoAsset() {
        val scenario = createScenario(releaseSizeOverride = 1L)
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertFalse(scenario.updatesDirectory.listFiles().orEmpty().any { it.name.endsWith(".part") })
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaPackageNameIncorretoDentroDoApk() {
        val testApk = File(
            InstrumentationRegistry.getInstrumentation().context.applicationInfo.sourceDir,
        ).readBytes()
        val scenario = createScenario(servedApkBytes = testApk)
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertEquals(2, scenario.server.requestCount)
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaVersaoDeclaradaNoManifestoDiferenteDaRelease() {
        val scenario = createScenario { manifest -> manifest.put("versionName", "99.0.0") }
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertEquals(1, scenario.server.requestCount)
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaCertificadoDeAssinaturaDiferenteDaInstalacao() {
        val scenario = createScenario { manifest -> manifest.put("signingCertificateSha256", "A".repeat(64)) }
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertEquals(1, scenario.server.requestCount)
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaMinSdkInternoDiferenteDoManifesto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actualMinSdk = context.applicationInfo.minSdkVersion
        assumeTrue(Build.VERSION.SDK_INT > actualMinSdk)
        val scenario = createScenario { manifest -> manifest.put("minSdk", actualMinSdk + 1) }
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertEquals(2, scenario.server.requestCount)
        } finally {
            scenario.server.shutdown()
        }
    }

    @Test
    fun rejeitaMinSdkAcimaDoDispositivoAntesDoDownload() {
        val scenario = createScenario { manifest -> manifest.put("minSdk", Build.VERSION.SDK_INT + 1) }
        try {
            expectIOException { scenario.repository.downloadAndVerify(scenario.release) {} }
            assertEquals(1, scenario.server.requestCount)
        } finally {
            scenario.server.shutdown()
        }
    }

    private data class Scenario(
        val server: MockWebServer,
        val repository: AppUpdateRepository,
        val release: AvailableRelease,
        val apkBytes: ByteArray,
        val updatesDirectory: File,
    )

    private fun createScenario(
        servedApkBytes: ByteArray? = null,
        manifestDigestOverride: String? = null,
        releaseSizeOverride: Long? = null,
        mutateManifest: (JSONObject) -> Unit = {},
    ): Scenario {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val installedRepository = AppUpdateRepository(context)
        val installed = installedRepository.installedVersion()
        val targetApk = File(context.applicationInfo.sourceDir).readBytes()
        val apkBytes = servedApkBytes ?: targetApk
        val assetName = "gemini-live-${installed.versionName}.apk"
        val manifest = JSONObject()
            .put("schemaVersion", 1)
            .put("packageName", context.packageName)
            .put("versionName", installed.versionName)
            .put("versionCode", installed.versionCode)
            .put("minSdk", context.applicationInfo.minSdkVersion)
            .put("apkAsset", assetName)
            .put("apkSha256", sha256(apkBytes))
            .put("signingCertificateSha256", installed.signingCertificateSha256)
        mutateManifest(manifest)
        val manifestBytes = manifest.toString().toByteArray(Charsets.UTF_8)

        val heldCertificate = HeldCertificate.Builder()
            .addSubjectAlternativeName("localhost")
            .addSubjectAlternativeName("127.0.0.1")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(heldCertificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(heldCertificate.certificate)
            .build()

        val server = MockWebServer()
        server.useHttps(serverCertificates.sslSocketFactory(), false)
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(manifestBytes)),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(apkBytes)),
        )

        val client = OkHttpClient.Builder()
            .sslSocketFactory(clientCertificates.sslSocketFactory(), clientCertificates.trustManager)
            .build()
        val olderInstalledVersion = installed.copy(versionCode = installed.versionCode - 1)
        val repository = AppUpdateRepository(context, client) { olderInstalledVersion }
        val release = AvailableRelease(
            tagName = "v${installed.versionName}",
            versionName = installed.versionName,
            versionCode = installed.versionCode,
            publishedAt = "2026-11-03T00:00:00Z",
            notes = "Instrumented test",
            apkAssetName = assetName,
            apkUrl = server.url("/apk").toString(),
            apkSize = releaseSizeOverride ?: apkBytes.size.toLong(),
            manifestUrl = server.url("/manifest").toString(),
            manifestDigest = manifestDigestOverride ?: "sha256:${sha256(manifestBytes)}",
        )
        return Scenario(
            server = server,
            repository = repository,
            release = release,
            apkBytes = apkBytes,
            updatesDirectory = File(context.filesDir, "updates"),
        )
    }

    private fun expectIOException(block: () -> Unit) {
        try {
            block()
            throw AssertionError("Era esperada uma IOException, mas a operação foi aceita.")
        } catch (_: IOException) {
            // Resultado esperado: o artefato não passou na validação.
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
