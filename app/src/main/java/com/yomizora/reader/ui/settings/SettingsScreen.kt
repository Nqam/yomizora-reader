package com.yomizora.reader.ui.settings

import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.yomizora.reader.ui.components.JapaneseVoiceOption
import com.yomizora.reader.ui.components.applyJapaneseTtsSettings
import com.yomizora.reader.ui.components.listJapaneseVoices
import com.yomizora.reader.ui.components.openInstallTtsData
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomizora.reader.BuildConfig
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.ThemeMode
import com.yomizora.reader.domain.model.UserProfile

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val importReport by viewModel.importReport.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isCheckingAppUpdate by viewModel.isCheckingAppUpdate.collectAsStateWithLifecycle()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsStateWithLifecycle()
    val appDownloadProgress by viewModel.appDownloadProgress.collectAsStateWithLifecycle()
    val isCheckingContentUpdate by viewModel.isCheckingContentUpdate.collectAsStateWithLifecycle()
    val remoteContentUpdates by viewModel.remoteContentUpdates.collectAsStateWithLifecycle()
    val isDownloadingContent by viewModel.isDownloadingContent.collectAsStateWithLifecycle()

    var pendingResetLevel by remember { mutableStateOf<JlptLevel?>(null) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var renamingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var deletingProfile by remember { mutableStateOf<UserProfile?>(null) }
    var profileNameInput by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsAvailable by remember { mutableStateOf(false) }
    var japaneseVoices by remember { mutableStateOf<List<JapaneseVoiceOption>>(emptyList()) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsAvailable = engine?.applyJapaneseTtsSettings(settings) == true
                japaneseVoices = engine?.listJapaneseVoices().orEmpty()
            }
        }
        ttsEngine = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackupToUri(uri)
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackupFromUri(uri)
    }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                SettingsSectionCard(title = "Профили пользователей") {
                    Text(
                        text = "Для каждого профиля прогресс чтения, статистика и прохождение уровней сохраняются отдельно.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    profiles.forEach { profile ->
                        val isSelected = activeProfile?.id == profile.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.switchProfile(profile) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.switchProfile(profile) }
                            )
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    profileNameInput = profile.name
                                    renamingProfile = profile
                                }
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Переименовать")
                            }
                            if (profiles.size > 1) {
                                IconButton(onClick = { deletingProfile = profile }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Удалить")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            profileNameInput = ""
                            showCreateProfileDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Добавить профиль")
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Чтение и отображение") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Фуригана по умолчанию", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.showFurigana,
                            onCheckedChange = viewModel::setShowFurigana
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Скрывать знакомую фуригану", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Скрывать подсказки для слов, встреченных в 2+ пройденных текстах",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.progressiveFurigana,
                            onCheckedChange = viewModel::setProgressiveFurigana
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Размер шрифта: ${settings.fontSizeSp.toInt()} sp",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.fontSizeSp,
                        onValueChange = viewModel::setFontSize,
                        valueRange = 14f..30f
                    )

                    Text(
                        text = "Межстрочный интервал: ${"%.2f".format(settings.lineHeightMultiplier)}x",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.lineHeightMultiplier,
                        onValueChange = viewModel::setLineHeight,
                        valueRange = 1.3f..2.2f
                    )

                    Text("Режим текста", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    val modes = listOf(
                        TextDisplayMode.KANJI_WITH_FURIGANA to "Кандзи+",
                        TextDisplayMode.KANJI_ONLY to "Кандзи",
                        TextDisplayMode.KANA_ONLY to "Кана"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        modes.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = settings.displayMode == mode,
                                onClick = { viewModel.setDisplayMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                            ) {
                                Text(label)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Тема оформления", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    val themes = listOf(
                        ThemeMode.SYSTEM to "Авто",
                        ThemeMode.LIGHT to "Светлая",
                        ThemeMode.DARK to "Тёмная"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themes.forEachIndexed { index, (theme, label) ->
                            SegmentedButton(
                                selected = settings.themeMode == theme,
                                onClick = { viewModel.setThemeMode(theme) },
                                shape = SegmentedButtonDefaults.itemShape(index, themes.size)
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Озвучка текста (TTS)") {
                    val hasOfflineVoice = japaneseVoices.any { it.isOffline }
                    Text(
                        text = when {
                            ttsAvailable && hasOfflineVoice ->
                                "Японский голос готов (доступен офлайн-режим без интернета)."
                            ttsAvailable ->
                                "Японский голос активен. Для работы без интернета установите офлайн-пакет."
                            else ->
                                "Японский голосовой пакет не найден на устройстве."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { openInstallTtsData(context) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Установить японский пакет (офлайн)")
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Скорость речи: ${String.format(Locale.US, "%.2fx", settings.ttsSpeechRate)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.ttsSpeechRate,
                        onValueChange = viewModel::setTtsSpeechRate,
                        valueRange = 0.5f..1.6f
                    )
                    Text(
                        text = "Тембр голоса: ${String.format(Locale.US, "%.2fx", settings.ttsPitch)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = settings.ttsPitch,
                        onValueChange = viewModel::setTtsPitch,
                        valueRange = 0.75f..1.35f
                    )
                    if (japaneseVoices.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Выбор голоса",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setTtsVoiceName(null) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = settings.ttsVoiceName == null,
                                onClick = { viewModel.setTtsVoiceName(null) }
                            )
                            Text("Авто (приоритет офлайн-голоса)", style = MaterialTheme.typography.bodyMedium)
                        }
                        japaneseVoices.take(6).forEach { voiceOpt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setTtsVoiceName(voiceOpt.name) }
                                    .padding(vertical = 2.dp)
                            ) {
                                RadioButton(
                                    selected = settings.ttsVoiceName == voiceOpt.name,
                                    onClick = { viewModel.setTtsVoiceName(voiceOpt.name) }
                                )
                                Text(voiceOpt.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            ttsEngine?.applyJapaneseTtsSettings(settings)
                            ttsEngine?.speak(
                                "こんにちは。日本語の読み上げテストです。",
                                TextToSpeech.QUEUE_FLUSH,
                                null,
                                "tts_preview"
                            )
                        },
                        enabled = ttsAvailable,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Проверить голос")
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Обновление приложения (GitHub)") {
                    Text(
                        text = "Текущая версия: v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::checkAppUpdate,
                        enabled = !isCheckingAppUpdate && appDownloadProgress == null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCheckingAppUpdate) "Проверка..." else "Проверить обновления")
                    }
                    if (appUpdateInfo != null) {
                        val info = appUpdateInfo!!
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Доступна версия v${info.versionName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = info.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (appDownloadProgress != null) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { appDownloadProgress!! },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::downloadAndInstallAppUpdate,
                            enabled = appDownloadProgress == null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (appDownloadProgress != null) {
                                    "Загрузка ${(appDownloadProgress!! * 100).toInt()}%..."
                                } else {
                                    "Скачать и установить"
                                }
                            )
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Обновление контента (GitHub)") {
                    Text(
                        text = "Новые тексты загружаются с проверкой на дубликаты и без потери прогресса.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::checkContentUpdates,
                        enabled = !isCheckingContentUpdate && !isDownloadingContent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isCheckingContentUpdate) "Проверка..." else "Проверить новый контент")
                    }
                    if (remoteContentUpdates.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        remoteContentUpdates.forEach { pack ->
                            Text(
                                text = "• ${pack.title} (v${pack.packVersion})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::downloadAndImportRemoteContent,
                            enabled = !isDownloadingContent,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isDownloadingContent) "Загрузка..." else "Загрузить и добавить")
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Резервное копирование") {
                    OutlinedButton(
                        onClick = { backupLauncher.launch("yomizora_progress_backup.json") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Резервная копия прогресса")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json")) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Восстановить прогресс из копии")
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Сброс прогресса") {
                    Text(
                        text = "Сброс очищает прогресс только выбранного уровня. Другие уровни и настройки остаются без изменений.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    JlptLevel.activeLevels.forEach { level ->
                        OutlinedButton(
                            onClick = { pendingResetLevel = level },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Сбросить прогресс ${level.code}")
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "О приложении") {
                    Text(
                        text = "Yomizora (読み空) • Версия ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Полностью автономное приложение для практики чтения на японском языке (JLPT N5–N4).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (pendingResetLevel != null) {
        val level = pendingResetLevel!!
        AlertDialog(
            onDismissRequest = { pendingResetLevel = null },
            title = { Text("Сбросить прогресс ${level.code}?") },
            text = {
                Text(
                    "Это действие необратимо. Все тексты уровня ${level.code}, кроме первого, снова станут заблокированы, " +
                        "а статистика прохождения ${level.code} обнулится. Другие уровни сохранятся."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetProgress(level)
                        pendingResetLevel = null
                    }
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingResetLevel = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (importReport != null) {
        val r = importReport!!
        AlertDialog(
            onDismissRequest = viewModel::dismissImportReport,
            title = { Text("Результат импорта") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Пакет: ${r.packId}")
                    Text("Добавлено: ${r.insertedCount}")
                    Text("Обновлено: ${r.updatedCount}")
                    Text("Пропущено: ${r.skippedCount}")
                    Text("Отклонено: ${r.rejectedCount}")
                    if (r.errors.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = r.errors.take(5).joinToString("\n"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissImportReport) {
                    Text("ОК")
                }
            }
        )
    }

    if (showCreateProfileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateProfileDialog = false },
            title = { Text("Новый профиль") },
            text = {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Имя профиля") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createProfile(profileNameInput)
                        showCreateProfileDialog = false
                    },
                    enabled = profileNameInput.isNotBlank()
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProfileDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (renamingProfile != null) {
        val target = renamingProfile!!
        AlertDialog(
            onDismissRequest = { renamingProfile = null },
            title = { Text("Переименовать профиль") },
            text = {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it },
                    label = { Text("Имя профиля") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameProfile(target.id, profileNameInput)
                        renamingProfile = null
                    },
                    enabled = profileNameInput.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingProfile = null }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (deletingProfile != null) {
        val target = deletingProfile!!
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text("Удалить профиль «${target.name}»?") },
            text = {
                Text("Весь прогресс этого профиля будет удалён без возможности восстановления.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfile(target)
                        deletingProfile = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
