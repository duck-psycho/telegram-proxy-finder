package com.duckpsycho.telegramproxyfinder.platform.update

import android.os.Handler
import android.os.Looper
import com.duckpsycho.telegramproxyfinder.BuildConfig
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseUpdate(
    val versionLabel: String,
    val pageUrl: String,
)

object GitHubReleaseChecker {
    private const val RELEASES_URL =
        "https://api.github.com/repos/duck-psycho/telegram-proxy-finder/releases"
    private val USER_AGENT = "Telegram-Proxy-Finder-Android/${BuildConfig.VERSION_NAME}"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 5_000

    fun checkAsync(onResult: (ReleaseUpdate?) -> Unit) {
        Thread {
            val update = runCatching { findNewerStableRelease(BuildConfig.VERSION_NAME) }.getOrNull()
            runCatching {
                Handler(Looper.getMainLooper()).post {
                    runCatching { onResult(update) }
                }
            }
        }.start()
    }

    private fun findNewerStableRelease(currentVersion: String): ReleaseUpdate? {
        val current = parseVersion(currentVersion) ?: return null
        val releases = fetchReleases() ?: return null

        return runCatching {
            findBestNewerRelease(releases, current)
        }.getOrNull()
    }

    private fun findBestNewerRelease(
        releases: JSONArray,
        current: List<Int>,
    ): ReleaseUpdate? {
        var best: ReleaseUpdate? = null
        var bestVersion: List<Int>? = null

        for (i in 0 until releases.length()) {
            val release = releases.optJSONObject(i) ?: continue
            if (release.optBoolean("prerelease", false)) continue

            val tagName = release.optString("tag_name", "")
            val pageUrl = release.optString("html_url", "")
            if (tagName.isEmpty() || pageUrl.isEmpty()) continue

            val version = parseVersion(tagName) ?: continue
            if (compareVersions(version, current) <= 0) continue

            if (bestVersion == null || compareVersions(version, bestVersion) > 0) {
                bestVersion = version
                best = ReleaseUpdate(versionLabel = tagName, pageUrl = pageUrl)
            }
        }

        return best
    }

    private fun fetchReleases(): JSONArray? {
        var connection: HttpURLConnection? = null
        return try {
            connection =
                (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", USER_AGENT)
                }

            if (connection.responseCode !in 200..299) {
                connection.errorStream?.close()
                return null
            }

            connection.inputStream.bufferedReader().use { reader ->
                JSONArray(reader.readText())
            }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseVersion(label: String): List<Int>? {
        val normalized = label.trim().removePrefix("v").removePrefix("V")
        if (normalized.isEmpty()) return null

        val parts = normalized.split('.')
        if (parts.isEmpty()) return null

        val numbers = mutableListOf<Int>()
        for (part in parts) {
            val digits = part.takeWhile { it.isDigit() }
            if (digits.isEmpty()) return null
            numbers.add(digits.toInt())
        }
        return numbers
    }

    private fun compareVersions(
        left: List<Int>,
        right: List<Int>,
    ): Int {
        val maxLength = maxOf(left.size, right.size)
        for (i in 0 until maxLength) {
            val leftPart = left.getOrElse(i) { 0 }
            val rightPart = right.getOrElse(i) { 0 }
            if (leftPart != rightPart) return leftPart.compareTo(rightPart)
        }
        return 0
    }
}
