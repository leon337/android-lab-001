package br.com.laboratorio.android.update

object AppVersioning {
    // Strict SemVer stable tags: no leading zeroes and no prerelease/build suffix.
    private val stableTag = Regex("^v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)$")

    fun parseStableTag(tag: String): String? {
        val match = stableTag.matchEntire(tag) ?: return null
        val versionName = "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}"
        return versionName.takeIf { versionCodeFor(it) != null }
    }

    /**
     * Monotonic Android versionCode for supported SemVer values.
     * MAJOR*10000 + MINOR*100 + PATCH; MINOR and PATCH must be below 100.
     */
    fun versionCodeFor(versionName: String): Long? {
        val parts = versionName.split(".")
        if (parts.size != 3) return null
        val major = parts[0].toLongOrNull() ?: return null
        val minor = parts[1].toLongOrNull() ?: return null
        val patch = parts[2].toLongOrNull() ?: return null
        if (major < 0 || minor !in 0..99 || patch !in 0..99) return null
        if (major > Int.MAX_VALUE.toLong() / 10_000L) return null

        val base = major * 10_000L
        val suffix = minor * 100L + patch
        if (base > Int.MAX_VALUE.toLong() - suffix) return null

        return (base + suffix).takeIf { it in 1..Int.MAX_VALUE.toLong() }
    }
}
