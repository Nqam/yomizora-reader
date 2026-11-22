package com.yomizora.reader.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.data.content.ImportReport
import com.yomizora.reader.data.update.AppUpdateCheckResult
import com.yomizora.reader.data.update.AppUpdateInfo
import com.yomizora.reader.data.update.ContentUpdateCheckResult
import com.yomizora.reader.data.update.GitHubUpdateManager
import com.yomizora.reader.data.update.RemoteContentPackInfo
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.ThemeMode
import com.yomizora.reader.domain.model.UserProfile
import com.yomizora.reader.domain.repository.ProgressRepository
import com.yomizora.reader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val progressRepository: ProgressRepository,
    private val updateManager: GitHubUpdateManager
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderSettings()
    )

    val profiles: StateFlow<List<UserProfile>> = progressRepository.observeProfiles().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val activeProfile: StateFlow<UserProfile?> = progressRepository.observeActiveProfile().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun createProfile(name: String) {
        viewModelScope.launch {
            val created = progressRepository.createProfile(name)
            _statusMessage.value = "Создан и выбран профиль: ${created.name}"
        }
    }

    fun switchProfile(profile: UserProfile) {
        viewModelScope.launch {
            progressRepository.switchProfile(profile.id)
            _statusMessage.value = "Активный профиль: ${profile.name}"
        }
    }

    fun renameProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            progressRepository.renameProfile(profileId, newName)
            _statusMessage.value = "Профиль переименован"
        }
    }

    fun deleteProfile(profile: UserProfile) {
        viewModelScope.launch {
            progressRepository.deleteProfile(profile.id)
            _statusMessage.value = "Профиль «${profile.name}» удалён"
        }
    }

    private val _importReport = MutableStateFlow<ImportReport?>(null)
    val importReport: StateFlow<ImportReport?> = _importReport.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isCheckingAppUpdate = MutableStateFlow(false)
    val isCheckingAppUpdate: StateFlow<Boolean> = _isCheckingAppUpdate.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private val _appDownloadProgress = MutableStateFlow<Float?>(null)
    val appDownloadProgress: StateFlow<Float?> = _appDownloadProgress.asStateFlow()

    private val _isCheckingContentUpdate = MutableStateFlow(false)
    val isCheckingContentUpdate: StateFlow<Boolean> = _isCheckingContentUpdate.asStateFlow()

    private val _remoteContentUpdates = MutableStateFlow<List<RemoteContentPackInfo>>(emptyList())
    val remoteContentUpdates: StateFlow<List<RemoteContentPackInfo>> = _remoteContentUpdates.asStateFlow()

    private val _isDownloadingContent = MutableStateFlow(false)
    val isDownloadingContent: StateFlow<Boolean> = _isDownloadingContent.asStateFlow()

    private val backupPrefs = context.getSharedPreferences("yomizora_backup_prefs", Context.MODE_PRIVATE)
    private val _linkedBackupName = MutableStateFlow(backupPrefs.getString("backup_name", null))
    val linkedBackupName: StateFlow<String?> = _linkedBackupName.asStateFlow()

    fun checkAppUpdate() {
        viewModelScope.launch {
            _isCheckingAppUpdate.value = true
            when (val res = updateManager.checkAppUpdate()) {
                is AppUpdateCheckResult.UpdateAvailable -> {
                    _appUpdateInfo.value = res.info
                    _statusMessage.value = "Найдена новая версия v${res.info.versionName}"
                }
                AppUpdateCheckResult.UpToDate -> {
                    _appUpdateInfo.value = null
                    _statusMessage.value = "Установлена актуальная версия приложения."
                }
                AppUpdateCheckResult.Offline -> {
                    _statusMessage.value = "Нет подключения к интернету."
                }
            }
            _isCheckingAppUpdate.value = false
        }
    }

    fun downloadAndInstallAppUpdate() {
        val info = _appUpdateInfo.value ?: return
        viewModelScope.launch {
            _appDownloadProgress.value = 0f
            val res = updateManager.downloadAndInstallApk(info.apkDownloadUrl) { p ->
                _appDownloadProgress.value = p
            }
            _appDownloadProgress.value = null
            if (res.isFailure) {
                _statusMessage.value = "Ошибка загрузки обновления: ${res.exceptionOrNull()?.localizedMessage ?: ""}"
            }
        }
    }

    fun checkContentUpdates() {
        viewModelScope.launch {
            _isCheckingContentUpdate.value = true
            when (val res = updateManager.checkContentUpdates()) {
                is ContentUpdateCheckResult.UpdatesAvailable -> {
                    _remoteContentUpdates.value = res.packs
                    _statusMessage.value = "Доступно пакетов контента: ${res.packs.size}"
                }
                ContentUpdateCheckResult.UpToDate -> {
                    _remoteContentUpdates.value = emptyList()
                    _statusMessage.value = "Контент уже обновлён до последней версии."
                }
                ContentUpdateCheckResult.Offline -> {
                    _statusMessage.value = "Нет подключения к интернету."
                }
            }
            _isCheckingContentUpdate.value = false
        }
    }

    fun downloadAndImportRemoteContent() {
        val packs = _remoteContentUpdates.value
        if (packs.isEmpty()) return
        viewModelScope.launch {
            _isDownloadingContent.value = true
            val res = updateManager.downloadAndImportContentPacks(packs)
            _isDownloadingContent.value = false
            res.onSuccess { reports ->
                _remoteContentUpdates.value = emptyList()
                val added = reports.sumOf { it.insertedCount }
                val updated = reports.sumOf { it.updatedCount }
                val skipped = reports.sumOf { it.skippedCount }
                val rejected = reports.sumOf { it.rejectedCount }
                _importReport.value = ImportReport(
                    packId = packs.joinToString(", ") { it.packId },
                    insertedCount = added,
                    updatedCount = updated,
                    skippedCount = skipped,
                    rejectedCount = rejected,
                    errors = reports.flatMap { it.errors }
                )
            }.onFailure {
                _statusMessage.value = "Ошибка загрузки контента: ${it.localizedMessage ?: ""}"
            }
        }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(sizeSp) }
    }

    fun setLineHeight(multiplier: Float) {
        viewModelScope.launch { settingsRepository.setLineHeightMultiplier(multiplier) }
    }

    fun setShowFurigana(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowFurigana(show) }
    }

    fun setProgressiveFurigana(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setProgressiveFurigana(enabled) }
    }

    fun setDisplayMode(mode: TextDisplayMode) {
        viewModelScope.launch { settingsRepository.setDisplayMode(mode) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setTtsSpeechRate(rate: Float) {
        viewModelScope.launch { settingsRepository.setTtsSpeechRate(rate) }
    }

    fun setTtsPitch(pitch: Float) {
        viewModelScope.launch { settingsRepository.setTtsPitch(pitch) }
    }

    fun setTtsVoiceName(voiceName: String?) {
        viewModelScope.launch { settingsRepository.setTtsVoiceName(voiceName) }
    }

    fun resetProgress(level: JlptLevel) {
        viewModelScope.launch {
            progressRepository.resetLevelProgress(level)
            _statusMessage.value = "Прогресс ${level.code} сброшен."
        }
    }

    fun exportBackupToLinkedOrPick(onNeedPicker: () -> Unit) {
        val savedUriStr = backupPrefs.getString("backup_uri", null)
        if (savedUriStr.isNullOrBlank()) {
            onNeedPicker()
            return
        }
        val uri = Uri.parse(savedUriStr)
        viewModelScope.launch {
            try {
                writeBackupToUri(uri)
                _statusMessage.value = "Копия перезаписана в «${_linkedBackupName.value ?: "выбранный файл"}»."
            } catch (_: Exception) {
                // Permission expired or file deleted — ask user to pick/create again
                onNeedPicker()
            }
        }
    }

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                linkBackupUri(uri)
                writeBackupToUri(uri)
                _statusMessage.value = "Резервная копия сохранена и привязана («${_linkedBackupName.value ?: "файл"}»)."
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка резервного копирования: ${e.localizedMessage ?: "Неизвестная ошибка"}"
            }
        }
    }

    fun restoreFromLinkedBackup() {
        val savedUriStr = backupPrefs.getString("backup_uri", null)
        if (savedUriStr.isNullOrBlank()) {
            _statusMessage.value = "Нет привязанного файла копии."
            return
        }
        importBackupFromUri(Uri.parse(savedUriStr), linkAfterImport = false)
    }

    fun importBackupFromUri(uri: Uri, linkAfterImport: Boolean = true) {
        viewModelScope.launch {
            try {
                val rawJson = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("Не удалось прочитать файл")
                }
                val restored = progressRepository.importProgressBackupJson(rawJson)
                if (linkAfterImport) {
                    linkBackupUri(uri)
                }
                _statusMessage.value = "Восстановлено записей прогресса: $restored"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка восстановления: ${e.localizedMessage ?: "Неверный формат файла"}"
            }
        }
    }

    private suspend fun writeBackupToUri(uri: Uri) {
        val backupJson = progressRepository.exportProgressBackupJson()
        withContext(Dispatchers.IO) {
            val os = runCatching { context.contentResolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Не удалось открыть файл для записи")
            os.bufferedWriter(Charsets.UTF_8).use { it.write(backupJson) }
        }
    }

    private fun linkBackupUri(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val name = resolveDisplayName(uri) ?: "yomizora_progress_backup.json"
        backupPrefs.edit()
            .putString("backup_uri", uri.toString())
            .putString("backup_name", name)
            .apply()
        _linkedBackupName.value = name
    }

    private fun resolveDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    fun dismissImportReport() {
        _importReport.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
