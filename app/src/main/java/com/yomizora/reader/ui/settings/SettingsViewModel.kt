package com.yomizora.reader.ui.settings

import android.content.Context
import android.net.Uri
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

    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val backupJson = progressRepository.exportProgressBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)
                        ?.use { it.write(backupJson) }
                        ?: throw IllegalStateException("Не удалось записать файл")
                }
                _statusMessage.value = "Резервная копия прогресса сохранена."
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка резервного копирования: ${e.localizedMessage ?: "Неизвестная ошибка"}"
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val rawJson = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: throw IllegalStateException("Не удалось прочитать файл")
                }
                val restored = progressRepository.importProgressBackupJson(rawJson)
                _statusMessage.value = "Восстановлено записей прогресса: $restored"
            } catch (e: Exception) {
                _statusMessage.value = "Ошибка восстановления: ${e.localizedMessage ?: "Неверный формат файла"}"
            }
        }
    }

    fun dismissImportReport() {
        _importReport.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
