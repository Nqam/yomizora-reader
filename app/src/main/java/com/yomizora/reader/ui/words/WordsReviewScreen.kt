package com.yomizora.reader.ui.words

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomizora.reader.ui.theme.LocalSemanticColors

@Composable
fun WordsReviewScreen(
    viewModel: WordsReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val semantic = LocalSemanticColors.current
    var showMasteredDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshDeck()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Проверка слов",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (state.totalWordsCount > 0 && !state.isFreePracticeMode) {
                OutlinedButton(
                    onClick = viewModel::startFreePracticeAll,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Повторить все")
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ReviewStatPill(
                label = "К повторению",
                value = "${state.dueWordsCount}",
                modifier = Modifier.weight(1f)
            )
            ReviewStatPill(
                label = "Всего слов",
                value = "${state.totalWordsCount}",
                modifier = Modifier.weight(1f)
            )
            ReviewStatPill(
                label = "Закреплено",
                value = "${state.masteredCount}",
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = state.masteredWords.isNotEmpty()) {
                        showMasteredDialog = true
                    }
            )
        }

        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.currentCard == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 420.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = if (state.totalWordsCount == 0) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                semantic.completedBg
                            },
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.totalWordsCount == 0) {
                                        Icons.Rounded.Style
                                    } else {
                                        Icons.Rounded.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = if (state.totalWordsCount == 0) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        semantic.completed
                                    },
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        Text(
                            text = if (state.totalWordsCount == 0) {
                                "Пока нет слов для повторения"
                            } else {
                                "Все слова повторены!"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (state.totalWordsCount == 0) {
                                "Пройдите хотя бы один текст в библиотеке, и слова из него автоматически появятся здесь для интервального повторения."
                            } else if (state.nextDueInHours != null) {
                                val h = state.nextDueInHours!!
                                val timeText = if (h >= 24) "${h / 24} дн." else "$h ч."
                                "Ближайшее повторение появится примерно через $timeText."
                            } else {
                                "Отличная работа! Возвращайтесь позже или прочитайте новый текст."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        if (state.totalWordsCount > 0) {
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = viewModel::startFreePracticeAll,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Replay, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Повторить все слова (${state.totalWordsCount})")
                            }
                        }
                    }
                }
            }

            else -> {
                val card = state.currentCard!!
                val wordItem = card.item

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (state.isFreePracticeMode) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Повторение всех слов • Осталось: ${state.freePracticeRemaining}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    TextButton(onClick = viewModel::exitFreePractice) {
                                        Text("Выйти")
                                    }
                                }
                            }
                        }
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (wordItem.reading != wordItem.word) {
                                    Text(
                                        text = wordItem.reading,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = wordItem.word,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = wordItem.partOfSpeech,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    val isMastered = wordItem.repetitions >= 3
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isMastered) {
                                            semantic.completedBg
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = if (isMastered) {
                                                "✓ Закреплено · Повторений: ${wordItem.repetitions}"
                                            } else {
                                                "Повторений: ${wordItem.repetitions} / 3"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (isMastered) {
                                                semantic.completed
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Выберите правильный перевод:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            card.options.forEachIndexed { idx, optText ->
                                val isSelected = state.selectedOptionIndex == idx
                                val isCorrectOpt = idx == card.correctIndex
                                val bg = when {
                                    state.isChecked && isCorrectOpt -> semantic.completedBg
                                    state.isChecked && isSelected && !isCorrectOpt -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val border = when {
                                    state.isChecked && isCorrectOpt -> semantic.completed
                                    state.isChecked && isSelected && !isCorrectOpt -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                }

                                Card(
                                    onClick = { viewModel.selectAndCheckOption(idx) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = bg),
                                    border = BorderStroke(1.5.dp, border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                                    )
                                }
                            }
                        }

                        if (state.isChecked) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (state.isCorrect) semantic.completedBg else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (state.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = if (state.isCorrect) semantic.completed else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (state.isCorrect) {
                                            if (state.isFreePracticeMode) {
                                                "Верно!"
                                            } else {
                                                "Запомнено! Следующее повторение через ${state.scheduledDays} дн."
                                            }
                                        } else {
                                            "Неверно. Правильный перевод: «${card.item.translationRu}». Слово вернётся для повторения."
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Button(
                                onClick = viewModel::nextCard,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Следующее слово")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMasteredDialog) {
        AlertDialog(
            onDismissRequest = { showMasteredDialog = false },
            title = { Text("Закреплённые слова (${state.masteredWords.size})") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.masteredWords, key = { it.wordId }) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val title = if (item.word == item.reading) {
                                    item.word
                                } else {
                                    "${item.word} (${item.reading})"
                                }
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.translationRu,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "×${item.repetitions}",
                                style = MaterialTheme.typography.labelMedium,
                                color = semantic.completed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMasteredDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun ReviewStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
