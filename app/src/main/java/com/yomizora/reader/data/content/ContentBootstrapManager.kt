package com.yomizora.reader.data.content

import android.content.Context
import com.yomizora.reader.BuildConfig
import com.yomizora.reader.data.local.content.ContentDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentBootstrapManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentDatabase: ContentDatabase,
    private val contentImporter: ContentImporter,
    private val json: Json
) {
    private val mutex = Mutex()
    @Volatile
    private var initialized = false

    suspend fun ensureStarterContentLoaded() {
        if (initialized) return
        mutex.withLock {
            if (initialized) return
            withContext(Dispatchers.IO) {
                val assetFiles = context.assets.list("content")?.sorted().orEmpty()
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val signature = "${BuildConfig.VERSION_CODE}:${assetFiles.joinToString(",")}"
                val storedSignature = prefs.getString(KEY_BOOTSTRAP_SIGNATURE, null)

                if (storedSignature == signature && contentDatabase.materialDao().countByLevel("N5") > 0) {
                    initialized = true
                    return@withContext
                }

                val packDao = contentDatabase.contentPackDao()
                for (fileName in assetFiles) {
                    if (!fileName.endsWith(".json")) continue
                    val raw = context.assets.open("content/$fileName")
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val pkg = json.decodeFromString<ContentPackageDto>(raw)
                    val existingPack = packDao.getById(pkg.packId)
                    if (existingPack == null || pkg.packVersion > existingPack.version) {
                        contentImporter.importPackage(pkg)
                    }
                }
                prefs.edit().putString(KEY_BOOTSTRAP_SIGNATURE, signature).apply()
                initialized = true
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "yomizora_bootstrap_prefs"
        const val KEY_BOOTSTRAP_SIGNATURE = "bootstrap_signature"
    }
}
