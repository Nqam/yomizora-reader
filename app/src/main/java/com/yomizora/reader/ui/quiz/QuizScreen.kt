package com.yomizora.reader.ui.quiz

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomizora.reader.ui.components.FuriganaInlineRow
import com.yomizora.reader.ui.theme.LocalSemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    onOpenNextMaterial: (String) -> Unit,
    onBackToLibrary: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val semantic = LocalSemanticColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.material?.let {
                            "Проверка • #${it.orderIndex.toString().padStart(2, '0')}"
                        } ?: "Проверка"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.isLocked || state.material == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Материал недоступен", style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = onBack) {
                            Text("Назад")
                        }
                    }
                }
            }

            state.isQuizCompleted -> {
                val next = state.nextMaterial
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 460.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = semantic.completedBg,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = semantic.completed,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                        Text(
                            text = "Текст успешно пройден!",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (next != null) {
                                "Открыт следующий материал: #${next.orderIndex.toString().padStart(2, '0')} «${next.title}»"
                            } else {
                                "Поздравляем! Вы прошли все доступные тексты этого уровня."
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (next != null) {
                            Button(
                                onClick = { onOpenNextMaterial(next.id) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Перейти к следующему тексту")
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                            }
                        }
                        OutlinedButton(
                            onClick = onBackToLibrary,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Вернуться в библиотеку")
                        }
                    }
                }
            }

            else -> {
                val material = state.material!!
                val questions = material.questions
                val question = questions[state.currentQuestionIndex]
                val progress = (state.currentQuestionIndex + 1).toFloat() / questions.size.toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 620.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Вопрос ${state.currentQuestionIndex + 1} из ${questions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                        }

                        val qSegs = question.questionSegments
                        if (!qSegs.isNullOrEmpty()) {
                            FuriganaInlineRow(
                                segments = qSegs,
                                settings = settings,
                                familiarWords = state.familiarWords,
                                fontSizeSp = 21f,
                                textColor = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = question.question,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            question.options.forEachIndexed { idx, optionText ->
                                val isSelected = state.selectedOptionIndex == idx
                                val isCorrectOption = idx == question.correctAnswer

                                val borderColor = when {
                                    state.isAnswerChecked && isSelected && state.isCurrentAnswerCorrect ->
                                        semantic.completed
                                    state.isAnswerChecked && isSelected && !state.isCurrentAnswerCorrect ->
                                        MaterialTheme.colorScheme.primary
                                    state.isAnswerChecked && !state.isCurrentAnswerCorrect && isCorrectOption ->
                                        semantic.completed.copy(alpha = 0.6f)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                }

                                val containerColor = when {
                                    state.isAnswerChecked && isSelected && state.isCurrentAnswerCorrect ->
                                        semantic.completedBg
                                    state.isAnswerChecked && isSelected && !state.isCurrentAnswerCorrect ->
                                        MaterialTheme.colorScheme.primaryContainer
                                    isSelected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                Card(
                                    onClick = { viewModel.selectOption(idx) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = containerColor),
                                    border = BorderStroke(1.5.dp, borderColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.selectOption(idx) }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val optSegs = question.optionSegments?.getOrNull(idx)
                                        if (!optSegs.isNullOrEmpty()) {
                                            FuriganaInlineRow(
                                                segments = optSegs,
                                                settings = settings,
                                                familiarWords = state.familiarWords,
                                                fontSizeSp = 17f,
                                                textColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        } else {
                                            Text(
                                                text = optionText,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (state.isAnswerChecked) {
                            val isCorrect = state.isCurrentAnswerCorrect
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCorrect) {
                                        semantic.completedBg
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isCorrect) {
                                                Icons.Rounded.CheckCircle
                                            } else {
                                                Icons.Rounded.Cancel
                                            },
                                            contentDescription = null,
                                            tint = if (isCorrect) {
                                                semantic.completed
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isCorrect) "Правильно!" else "Неправильно",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = question.explanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!question.referenceExcerpt.isNullOrBlank()) {
                                        Text(
                                            text = "Фрагмент текста: «${question.referenceExcerpt}»",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        if (!state.isAnswerChecked) {
                            Button(
                                onClick = viewModel::checkAnswer,
                                enabled = state.selectedOptionIndex != null,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Проверить ответ")
                            }
                        } else if (state.isCurrentAnswerCorrect) {
                            Button(
                                onClick = viewModel::advanceOrFinish,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    if (state.currentQuestionIndex + 1 < questions.size) {
                                        "Следующий вопрос"
                                    } else {
                                        "Завершить проверку"
                                    }
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = viewModel::retryCurrentQuestion,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Попробовать снова")
                            }
                        }
                    }
                }
            }
        }
    }
}
