package com.yomizora.reader.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.yomizora.reader.BuildConfig
import com.yomizora.reader.data.content.ContentImporter
import com.yomizora.reader.data.content.ImportReport
import com.yomizora.reader.data.local.content.ContentDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    @SerialName("browser_download_url")
    val downloadUrl: String
)

@Serializable
data class ContentManifestDto(
    @SerialName("manifest_version")
    val manifestVersion: Int = 1,
    val packs: List<RemoteContentPackInfo> = emptyList()
)

@Serializable
data class RemoteContentPackInfo(
    @SerialName("pack_id")
    val packId: String,
    @SerialName("pack_version")
    val packVersion: Int,
    val title: String,
    val description: String = "",
    val url: String
)

data class AppUpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String
)

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateCheckResult
    data object UpToDate : AppUpdateCheckResult
    data object Offline : AppUpdateCheckResult
}

sealed interface ContentUpdateCheckResult {
    data class UpdatesAvailable(val packs: List<RemoteContentPackInfo>) : ContentUpdateCheckResult
    data object UpToDate : ContentUpdateCheckResult
    data object Offline : ContentUpdateCheckResult
}

@Singleton
class GitHubUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val contentImporter: ContentImporter,
    private val json: Json
) {
    private val owner = BuildConfig.GITHUB_OWNER
    private val repo = BuildConfig.GITHUB_REPO

    suspend fun checkAppUpdate(): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val raw = httpGetString(apiUrl, accept = "application/vnd.github+json")
                ?: return@withContext AppUpdateCheckResult.Offline

            val release = json.decodeFromString<GitHubReleaseDto>(raw)
            val remoteVersion = release.tagName.trim().removePrefix("v").removePrefix("V")
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return@withContext AppUpdateCheckResult.UpToDate

            if (isNewerVersion(remoteVersion, BuildConfig.VERSION_NAME)) {
                AppUpdateCheckResult.UpdateAvailable(
                    AppUpdateInfo(
                        versionName = remoteVersion,
                        releaseNotes = release.body?.takeIf { it.isNotBlank() }
                            ?: "Доступна новая версия приложения.",
                        apkDownloadUrl = apkAsset.downloadUrl
                    )
                )
            } else {
                AppUpdateCheckResult.UpToDate
            }
        } catch (_: Exception) {
            AppUpdateCheckResult.Offline
        }
    }

    suspend fun downloadAndInstallApk(
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "yomizora-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Yomizora-Android/${BuildConfig.VERSION_NAME}")
            }
            conn.connect()
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }

            val totalBytes = conn.contentLengthLong.coerceAtLeast(1L)
            var downloaded = 0L
            conn.inputStream.use { input ->
                apkFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress((downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f))
                    }
                }
            }
            conn.disconnect()
            onProgress(1f)

            withContext(Dispatchers.Main) {
                launchApkInstaller(apkFile)
            }
        }
    }

    private fun launchApkInstaller(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permIntent)
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }

    suspend fun checkContentUpdates(): ContentUpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val manifestUrl =
                "https://raw.githubusercontent.com/$owner/$repo/main/content_updates/manifest.json"
            val raw = httpGetString(manifestUrl)
                ?: return@withContext ContentUpdateCheckResult.Offline

            val manifest = json.decodeFromString<ContentManifestDto>(raw)
            val packDao = contentDatabase.contentPackDao()
            val updates = manifest.packs.filter { remote ->
                val existing = packDao.getById(remote.packId)
                existing == null || remote.packVersion > existing.version
            }
            if (updates.isNotEmpty()) {
                ContentUpdateCheckResult.UpdatesAvailable(updates)
            } else {
                ContentUpdateCheckResult.UpToDate
            }
        } catch (_: Exception) {
            ContentUpdateCheckResult.Offline
        }
    }

    suspend fun downloadAndImportContentPacks(
        packs: List<RemoteContentPackInfo>
    ): Result<List<ImportReport>> = withContext(Dispatchers.IO) {
        runCatching {
            val reports = mutableListOf<ImportReport>()
            for (pack in packs) {
                val rawJson = httpGetString(pack.url)
                    ?: throw IllegalStateException("Не удалось скачать пакет ${pack.title}")
                val report = contentImporter.importPackageJson(rawJson)
                reports += report
            }
            reports
        }
    }

    private fun httpGetString(urlStr: String, accept: String = "application/json"): String? {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            connectTimeout = 6_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", accept)
            setRequestProperty("User-Agent", "Yomizora-Android/${BuildConfig.VERSION_NAME}")
        }
        return try {
            conn.connect()
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }

    internal fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val lParts = local.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}
