package br.com.laboratorio.android.update

object AppVersioning {
    private val stableTag = Regex("^v(\\d+)\\.(\\d+)\\.(\\d+)$")

    fun parseStableTag(tag: String): String? {
        val match = stableTag.matchEntire(tag) ?: return null
        return "${match.groupValues[1]}.${match.groupValues[2]}.${match.groupValues[3]}"
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
        val code = major * 10_000L + minor * 100L + patch
        return code.takeIf { it in 1..Int.MAX_VALUE.toLong() }
    }
}
