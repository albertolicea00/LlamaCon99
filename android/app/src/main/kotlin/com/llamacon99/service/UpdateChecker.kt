package com.llamacon99.service

import com.llamacon99.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val htmlUrl: String,
    val apkUrl: String?,
)

sealed interface UpdateCheckResult {
    data class Available(val info: UpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data object Failed : UpdateCheckResult
}

object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/albertolicea00/LlamaCon99/releases"

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val conn = URL(RELEASES_URL).openConnection() as HttpURLConnection
            val body = try {
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext UpdateCheckResult.Failed
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val releases = JSONArray(body)
            if (releases.length() == 0) return@withContext UpdateCheckResult.UpToDate

            val latest = releases.getJSONObject(0)
            val tagName = latest.getString("tag_name").removePrefix("v")
            val htmlUrl = latest.getString("html_url")

            var apkUrl: String? = null
            val assets = latest.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name").lowercase()
                if (name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (!isNewer(tagName, BuildConfig.VERSION_NAME)) return@withContext UpdateCheckResult.UpToDate

            UpdateCheckResult.Available(UpdateInfo(
                versionName = tagName,
                htmlUrl = htmlUrl,
                apkUrl = apkUrl,
            ))
        } catch (_: Exception) {
            UpdateCheckResult.Failed
        }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").mapNotNull { it.toIntOrNull() }
        val l = local.split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }
}
