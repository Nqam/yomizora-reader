package com.yomizora.reader.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.data.content.ContentBootstrapManager
import com.yomizora.reader.data.update.AppUpdateCheckResult
import com.yomizora.reader.data.update.AppUpdateInfo
import com.yomizora.reader.data.update.ContentUpdateCheckResult
import com.yomizora.reader.data.update.GitHubUpdateManager
import com.yomizora.reader.data.update.RemoteContentPackInfo
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bootstrapManager: ContentBootstrapManager,
    private val updateManager: GitHubUpdateManager,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _isContentReady = MutableStateFlow(false)
    val isContentReady: StateFlow<Boolean> = _isContentReady.asStateFlow()

    private val _availableAppUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableAppUpdate: StateFlow<AppUpdateInfo?> = _availableAppUpdate.asStateFlow()

    private val _apkDownloadProgress = MutableStateFlow<Float?>(null)
    val apkDownloadProgress: StateFlow<Float?> = _apkDownloadProgress.asStateFlow()

    private val _availableContentUpdates = MutableStateFlow<List<RemoteContentPackInfo>>(emptyList())
    val availableContentUpdates: StateFlow<List<RemoteContentPackInfo>> = _availableContentUpdates.asStateFlow()

    private val _isImportingRemoteContent = MutableStateFlow(false)
    val isImportingRemoteContent: StateFlow<Boolean> = _isImportingRemoteContent.asStateFlow()

    private val _updateBannerMessage = MutableStateFlow<String?>(null)
    val updateBannerMessage: StateFlow<String?> = _updateBannerMessage.asStateFlow()

    val settings: StateFlow<ReaderSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderSettings()
    )

    init {
        viewModelScope.launch {
            bootstrapManager.ensureStarterContentLoaded()
            _isContentReady.value = true
            checkUpdatesSilently()
        }
    }

    private fun checkUpdatesSilently() {
        viewModelScope.launch {
            val appRes = updateManager.checkAppUpdate()
            if (appRes is AppUpdateCheckResult.UpdateAvailable) {
                _availableAppUpdate.value = appRes.info
            }
            val contentRes = updateManager.checkContentUpdates()
            if (contentRes is ContentUpdateCheckResult.UpdatesAvailable) {
                _availableContentUpdates.value = contentRes.packs
            }
        }
    }

    fun startAppUpdateDownload() {
        val info = _availableAppUpdate.value ?: return
        viewModelScope.launch {
            _apkDownloadProgress.value = 0f
            val result = updateManager.downloadAndInstallApk(info.apkDownloadUrl) { p ->
                _apkDownloadProgress.value = p
            }
            _apkDownloadProgress.value = null
            if (result.isFailure) {
                _updateBannerMessage.value = "Не удалось скачать обновление."
            } else {
                _availableAppUpdate.value = null
            }
        }
    }

    fun dismissAppUpdateDialog() {
        _availableAppUpdate.value = null
    }

    fun downloadAndImportContentUpdates() {
        val packs = _availableContentUpdates.value
        if (packs.isEmpty()) return
        viewModelScope.launch {
            _isImportingRemoteContent.value = true
            val res = updateManager.downloadAndImportContentPacks(packs)
            _isImportingRemoteContent.value = false
            res.onSuccess { reports ->
                val added = reports.sumOf { it.insertedCount }
                val updated = reports.sumOf { it.updatedCount }
                val rejected = reports.sumOf { it.rejectedCount }
                _availableContentUpdates.value = emptyList()
                _updateBannerMessage.value =
                    "Контент обновлён: добавлено $added, обновлено $updated, отклонено дублей/ошибок: $rejected."
            }.onFailure {
                _updateBannerMessage.value = "Не удалось загрузить обновление контента."
            }
        }
    }

    fun dismissContentUpdateDialog() {
        _availableContentUpdates.value = emptyList()
    }

    fun clearBannerMessage() {
        _updateBannerMessage.value = null
    }
}
