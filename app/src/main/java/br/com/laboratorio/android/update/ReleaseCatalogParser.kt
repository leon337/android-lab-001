package br.com.laboratorio.android.update

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

object ReleaseCatalogParser {
    private const val MANIFEST_ASSET_NAME = "update-manifest.json"
    private const val RELEASE_ASSET_PREFIX =
        "/leon337/android-lab-001/releases/download/"
    private val sha256Digest = Regex("(?i)^sha256:[0-9a-f]{64}$")

    fun parse(raw: String): List<AvailableRelease> {
        val releases = JSONArray(raw)
        return buildList {
            for (index in 0 until releases.length()) {
                val release = releases.optJSONObject(index) ?: continue
                if (release.optBoolean("draft", false) || release.optBoolean("prerelease", false)) {
                    continue
                }

                val tag = release.optString("tag_name")
                val versionName = AppVersioning.parseStableTag(tag) ?: continue
                val versionCode = AppVersioning.versionCodeFor(versionName) ?: continue
                val assets = release.optJSONArray("assets") ?: continue
                var apk: JSONObject? = null
                var manifest: JSONObject? = null
                for (assetIndex in 0 until assets.length()) {
                    val asset = assets.optJSONObject(assetIndex) ?: continue
                    val name = asset.optString("name")
                    if (name.startsWith("gemini-live-") && name.endsWith(".apk")) apk = asset
                    if (name == MANIFEST_ASSET_NAME) manifest = asset
                }

                val apkAsset = apk ?: continue
                val manifestAsset = manifest ?: continue
                val apkUrl = apkAsset.optString("browser_download_url")
                val manifestUrl = manifestAsset.optString("browser_download_url")
                val manifestDigest = manifestAsset.optString("digest")
                val apkSize = apkAsset.optLong("size", 0L)

                if (!isTrustedReleaseAsset(apkUrl) || !isTrustedReleaseAsset(manifestUrl)) continue
                if (!sha256Digest.matches(manifestDigest)) continue
                if (apkSize <= 0L) continue

                add(
                    AvailableRelease(
                        tagName = tag,
                        versionName = versionName,
                        versionCode = versionCode,
                        publishedAt = release.optString("published_at"),
                        notes = release.optString("body"),
                        apkAssetName = apkAsset.optString("name"),
                        apkUrl = apkUrl,
                        apkSize = apkSize,
                        manifestUrl = manifestUrl,
                        manifestDigest = manifestDigest,
                    ),
                )
            }
        }.sortedByDescending { it.versionCode }
    }

    private fun isTrustedReleaseAsset(url: String): Boolean {
        val uri = Uri.parse(url)
        return uri.scheme?.equals("https", ignoreCase = true) == true &&
            uri.host?.equals("github.com", ignoreCase = true) == true &&
            uri.encodedPath.orEmpty().startsWith(RELEASE_ASSET_PREFIX)
    }
}
