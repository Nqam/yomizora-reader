package com.yomizora.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomizora.reader.ui.MainViewModel
import com.yomizora.reader.ui.navigation.YomizoraNavHost
import com.yomizora.reader.ui.theme.YomizoraTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val isReady by mainViewModel.isContentReady.collectAsStateWithLifecycle()
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val appUpdate by mainViewModel.availableAppUpdate.collectAsStateWithLifecycle()
            val downloadProgress by mainViewModel.apkDownloadProgress.collectAsStateWithLifecycle()
            val contentUpdates by mainViewModel.availableContentUpdates.collectAsStateWithLifecycle()
            val isImportingContent by mainViewModel.isImportingRemoteContent.collectAsStateWithLifecycle()
            val bannerMessage by mainViewModel.updateBannerMessage.collectAsStateWithLifecycle()

            YomizoraTheme(themeMode = settings.themeMode) {
                if (isReady) {
                    YomizoraNavHost()

                    if (appUpdate != null) {
                        val info = appUpdate!!
                        AlertDialog(
                            onDismissRequest = {
                                if (downloadProgress == null) mainViewModel.dismissAppUpdateDialog()
                            },
                            title = { Text("Доступно обновление v${info.versionName}") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = info.releaseNotes,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (downloadProgress != null) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress!! },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "Загрузка: ${(downloadProgress!! * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = mainViewModel::startAppUpdateDownload,
                                    enabled = downloadProgress == null
                                ) {
                                    Text(if (downloadProgress != null) "Загрузка..." else "Обновить")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = mainViewModel::dismissAppUpdateDialog,
                                    enabled = downloadProgress == null
                                ) {
                                    Text("Позже")
                                }
                            }
                        )
                    } else if (contentUpdates.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = {
                                if (!isImportingContent) mainViewModel.dismissContentUpdateDialog()
                            },
                            title = { Text("Доступен новый контент") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Можно загрузить новые тексты с GitHub без потери вашего прогресса:")
                                    contentUpdates.forEach { pack ->
                                        Text(
                                            text = "• ${pack.title}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    if (isImportingContent) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = mainViewModel::downloadAndImportContentUpdates,
                                    enabled = !isImportingContent
                                ) {
                                    Text(if (isImportingContent) "Загрузка..." else "Загрузить")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = mainViewModel::dismissContentUpdateDialog,
                                    enabled = !isImportingContent
                                ) {
                                    Text("Позже")
                                }
                            }
                        )
                    }

                    if (bannerMessage != null) {
                        AlertDialog(
                            onDismissRequest = mainViewModel::clearBannerMessage,
                            title = { Text("Обновление") },
                            text = { Text(bannerMessage!!) },
                            confirmButton = {
                                TextButton(onClick = mainViewModel::clearBannerMessage) {
                                    Text("ОК")
                                }
                            }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
