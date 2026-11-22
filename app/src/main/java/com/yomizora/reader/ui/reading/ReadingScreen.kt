package com.yomizora.reader.ui.reading

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yomizora.reader.domain.model.ContentCategory
import com.yomizora.reader.domain.model.MaterialStatus
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.ThemeMode
import com.yomizora.reader.ui.components.FuriganaParagraph
import com.yomizora.reader.ui.components.applyJapaneseTtsSettings
import com.yomizora.reader.ui.components.openInstallTtsData
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingScreen(
    onBack: () -> Unit,
    onStartQuiz: (String) -> Unit,
    viewModel: ReadingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showKeyVocab by remember { mutableStateOf(false) }
    var showKeyGrammar by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var ttsAvailable by remember { mutableStateOf(false) }
    var ttsInitialized by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId?.endsWith("_last") == true) {
                            isSpeaking = false
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        isSpeaking = false
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) {
                        isSpeaking = false
                    }
                })
                ttsAvailable = engine?.applyJapaneseTtsSettings(settings) == true
                ttsInitialized = true
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    LaunchedEffect(ttsInitialized, settings.ttsSpeechRate, settings.ttsPitch, settings.ttsVoiceName) {
        if (ttsInitialized) {
            ttsAvailable = tts?.applyJapaneseTtsSettings(settings) == true
        }
    }

    val scrollState = rememberScrollState()
    val scrollFraction = if (scrollState.maxValue > 0) {
        (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
    } else 1f

    LaunchedEffect(scrollFraction) {
        if (state.materialWithProgress != null && !state.isLocked) {
            delay(400L)
            viewModel.onScrollProgressChanged(scrollFraction)
        }
    }

    Scaffold(
        topBar = {
            val mat = state.materialWithProgress?.material
            TopAppBar(
                title = {
                    if (mat != null) {
                        Column {
                            Text(
                                text = "${mat.level.code} • #${mat.orderIndex.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ContentCategory.toRussian(mat.category),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (mat != null && !state.isLocked) {
                        if (ttsInitialized) {
                            IconButton(
                                onClick = {
                                    if (!ttsAvailable) {
                                        openInstallTtsData(context)
                                    } else if (isSpeaking) {
                                        tts?.stop()
                                        isSpeaking = false
                                    } else {
                                        val paragraphs = mat.paragraphs
                                            .map { p ->
                                                p.sentences.joinToString("") { s ->
                                                    s.segments.joinToString("") { it.text }
                                                }.trim()
                                            }
                                            .filter { it.isNotEmpty() }
                                        if (paragraphs.isNotEmpty()) {
                                            tts?.applyJapaneseTtsSettings(settings)
                                            paragraphs.forEachIndexed { idx, pText ->
                                                val mode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                                                val uid = if (idx == paragraphs.lastIndex) "${mat.id}_last" else "${mat.id}_$idx"
                                                tts?.speak(pText, mode, null, uid)
                                            }
                                            isSpeaking = true
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = if (ttsAvailable) "Озвучить текст" else "Установить японский голос",
                                    tint = if (ttsAvailable) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Rounded.TextFields, contentDescription = "Настройки чтения")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val item = state.materialWithProgress
            if (item != null && !state.isLocked) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (scrollState.maxValue > 0) {
                            LinearProgressIndicator(
                                progress = { scrollFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { onStartQuiz(item.material.id) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text(
                                    text = if (item.status == MaterialStatus.COMPLETED) {
                                        "Проверить понимание ещё раз"
                                    } else {
                                        "Перейти к проверочным вопросам"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
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

            state.isLocked -> {
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Материал заблокирован",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Сначала успешно пройдите предыдущий текст этого уровня.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onBack) {
                            Text("Вернуться")
                        }
                    }
                }
            }

            state.materialWithProgress == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Материал не найден")
                }
            }

            else -> {
                val material = state.materialWithProgress!!.material
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 22.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = material.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = material.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                        if (state.keyVocabulary.isNotEmpty()) {
                            Card(
                                onClick = { showKeyVocab = !showKeyVocab },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ключевые слова текста (${state.keyVocabulary.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            imageVector = if (showKeyVocab) {
                                                Icons.Rounded.ExpandLess
                                            } else {
                                                Icons.Rounded.ExpandMore
                                            },
                                            contentDescription = null
                                        )
                                    }
                                    AnimatedVisibility(visible = showKeyVocab) {
                                        Column(
                                            modifier = Modifier.padding(top = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            state.keyVocabulary.forEach { entry ->
                                                val isConversational = entry.partOfSpeech.lowercase().let {
                                                    it.contains("разговор") || it.contains("выражен") || it.contains("идиом")
                                                }
                                                val label = if (entry.word == entry.reading) {
                                                    entry.word
                                                } else {
                                                    "${entry.word} (${entry.reading})"
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = if (isConversational) "💬 $label" else label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isConversational) {
                                                            MaterialTheme.colorScheme.tertiary
                                                        } else {
                                                            MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                                    Text(
                                                        text = entry.translationRu,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        textAlign = TextAlign.End,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (state.grammarPoints.isNotEmpty()) {
                            Card(
                                onClick = { showKeyGrammar = !showKeyGrammar },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.School,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Ключевая грамматика текста (${state.grammarPoints.size})",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Icon(
                                            imageVector = if (showKeyGrammar) {
                                                Icons.Rounded.ExpandLess
                                            } else {
                                                Icons.Rounded.ExpandMore
                                            },
                                            contentDescription = null
                                        )
                                    }
                                    AnimatedVisibility(visible = showKeyGrammar) {
                                        Column(
                                            modifier = Modifier.padding(top = 10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            state.grammarPoints.forEach { gp ->
                                                Surface(
                                                    onClick = { viewModel.onGrammarClick(gp) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = gp.pattern,
                                                                style = MaterialTheme.typography.titleMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.secondary
                                                            )
                                                            Text(
                                                                text = gp.level.code,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Text(
                                                            text = gp.titleRu,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = gp.formula,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        material.paragraphs.forEachIndexed { pIdx, paragraph ->
                            FuriganaParagraph(
                                paragraphIndex = pIdx,
                                paragraph = paragraph,
                                settings = settings,
                                familiarWords = state.familiarWords,
                                underlinedGrammarPositions = state.underlinedGrammarPositions,
                                selectedSegment = state.selectedWord?.segment,
                                onSegmentClick = viewModel::onSegmentClick,
                                onGrammarClick = viewModel::onGrammarClick
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (state.selectedWord != null) {
        val wordPopup = state.selectedWord!!
        ModalBottomSheet(onDismissRequest = viewModel::dismissWordPopup) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val wordTitle = wordPopup.entry?.word ?: wordPopup.segment.text
                val reading = wordPopup.entry?.reading ?: wordPopup.segment.reading
                Column {
                    Text(
                        text = wordTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!reading.isNullOrBlank()) {
                        Text(
                            text = reading,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (wordPopup.entry != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = wordPopup.entry.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = wordPopup.entry.translationRu,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!wordPopup.entry.note.isNullOrBlank()) {
                        Text(
                            text = wordPopup.entry.note,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "Перевод отсутствует в кратком словаре. Вы можете сохранить слово для повторения.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (state.selectedGrammar != null) {
        val gp = state.selectedGrammar!!
        ModalBottomSheet(onDismissRequest = viewModel::dismissGrammarPopup) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = gp.level.code,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = gp.pattern,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = gp.titleRu,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = gp.formula,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Text(
                    text = gp.explanationRu,
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = gp.exampleJp,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = gp.exampleRu,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        ReaderDisplaySettingsSheet(
            settings = settings,
            onDismiss = { showSettingsSheet = false },
            onFontSizeChange = viewModel::updateFontSize,
            onLineHeightChange = viewModel::updateLineHeight,
            onShowFuriganaChange = viewModel::updateShowFurigana,
            onProgressiveFuriganaChange = viewModel::updateProgressiveFurigana,
            onDisplayModeChange = viewModel::updateDisplayMode,
            onThemeModeChange = viewModel::updateThemeMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderDisplaySettingsSheet(
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLineHeightChange: (Float) -> Unit,
    onShowFuriganaChange: (Boolean) -> Unit,
    onProgressiveFuriganaChange: (Boolean) -> Unit,
    onDisplayModeChange: (TextDisplayMode) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Настройки отображения",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Показывать фуригану", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = settings.showFurigana,
                    onCheckedChange = onShowFuriganaChange
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text("Скрывать знакомую фуригану", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Для слов из 2+ пройденных текстов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.progressiveFurigana,
                    onCheckedChange = onProgressiveFuriganaChange
                )
            }

            Column {
                Text(
                    text = "Размер шрифта: ${settings.fontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.fontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = 14f..30f
                )
            }

            Column {
                Text(
                    text = "Межстрочный интервал: ${"%.2f".format(settings.lineHeightMultiplier)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = settings.lineHeightMultiplier,
                    onValueChange = onLineHeightChange,
                    valueRange = 1.3f..2.2f
                )
            }

            Text("Режим текста", style = MaterialTheme.typography.bodyMedium)
            val modes = listOf(
                TextDisplayMode.KANJI_WITH_FURIGANA to "Кандзи+",
                TextDisplayMode.KANJI_ONLY to "Кандзи",
                TextDisplayMode.KANA_ONLY to "Кана"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = settings.displayMode == mode,
                        onClick = { onDisplayModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Text("Тема оформления", style = MaterialTheme.typography.bodyMedium)
            val themes = listOf(
                ThemeMode.SYSTEM to "Авто",
                ThemeMode.LIGHT to "Светлая",
                ThemeMode.DARK to "Тёмная"
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                themes.forEachIndexed { index, (theme, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == theme,
                        onClick = { onThemeModeChange(theme) },
                        shape = SegmentedButtonDefaults.itemShape(index, themes.size)
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}
