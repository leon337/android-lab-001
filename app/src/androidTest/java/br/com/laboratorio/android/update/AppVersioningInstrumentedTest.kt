package br.com.laboratorio.android.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersioningInstrumentedTest {
    @Test
    fun tagEstavelEhInterpretadaEConvertidaEmVersionCodeMonotonico() {
        assertEquals("1.1.0", AppVersioning.parseStableTag("v1.1.0"))
        assertEquals(10_100L, AppVersioning.versionCodeFor("1.1.0"))
        assertEquals(10_200L, AppVersioning.versionCodeFor("1.2.0"))
        assertEquals(20_000L, AppVersioning.versionCodeFor("2.0.0"))
    }

    @Test
    fun tagsPrereleaseEValoresSemverForaDoContratoSaoRejeitados() {
        assertNull(AppVersioning.parseStableTag("v1.0.0-mvp.1"))
        assertNull(AppVersioning.parseStableTag("1.1.0"))
        assertNull(AppVersioning.versionCodeFor("1.100.0"))
        assertNull(AppVersioning.versionCodeFor("1.1"))
    }

    @Test
    fun catalogoAceitaSomenteReleasesEstaveisComApkEManifesto() {
        val raw = """
            [
              {
                "tag_name": "v1.1.0",
                "draft": false,
                "prerelease": false,
                "published_at": "2026-11-03T20:00:00Z",
                "body": "Primeira release da Fase 01",
                "assets": [
                  {
                    "name": "gemini-live-1.1.0.apk",
                    "browser_download_url": "https://github.com/leon337/android-lab-001/releases/download/v1.1.0/gemini-live-1.1.0.apk",
                    "size": 1024
                  },
                  {
                    "name": "update-manifest.json",
                    "browser_download_url": "https://github.com/leon337/android-lab-001/releases/download/v1.1.0/update-manifest.json",
                    "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                  }
                ]
              },
              {
                "tag_name": "v1.2.0-beta.1",
                "draft": false,
                "prerelease": true,
                "assets": []
              },
              {
                "tag_name": "v1.0.0",
                "draft": false,
                "prerelease": false,
                "assets": []
              }
            ]
        """.trimIndent()

        val releases = ReleaseCatalogParser.parse(raw)

        assertEquals(1, releases.size)
        assertEquals("v1.1.0", releases.single().tagName)
        assertEquals(10_100L, releases.single().versionCode)
        assertEquals("gemini-live-1.1.0.apk", releases.single().apkAssetName)
    }
}
