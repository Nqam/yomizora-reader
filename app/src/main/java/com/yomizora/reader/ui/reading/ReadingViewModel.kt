package com.yomizora.reader.ui.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.data.content.GrammarCatalog
import com.yomizora.reader.domain.model.DictionaryEntry
import com.yomizora.reader.domain.model.GrammarPoint
import com.yomizora.reader.domain.model.MaterialStatus
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.TextSegment
import com.yomizora.reader.domain.model.ThemeMode
import com.yomizora.reader.domain.repository.ContentRepository
import com.yomizora.reader.domain.repository.ProgressRepository
import com.yomizora.reader.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordPopupState(
    val segment: TextSegment,
    val entry: DictionaryEntry?
)

data class ReadingUiState(
    val isLoading: Boolean = true,
    val isLocked: Boolean = false,
    val materialWithProgress: MaterialWithProgress? = null,
    val selectedWord: WordPopupState? = null,
    val grammarPoints: List<GrammarPoint> = emptyList(),
    val selectedGrammar: GrammarPoint? = null,
    val keyVocabulary: List<DictionaryEntry> = emptyList(),
    val familiarWords: Set<Pair<String, String>> = emptySet()
)

@HiltViewModel
class ReadingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val materialId: String = checkNotNull(savedStateHandle["materialId"])

    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    val settings: StateFlow<ReaderSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderSettings()
    )

    init {
        loadMaterial()
    }

    private fun loadMaterial() {
        viewModelScope.launch {
            val item = progressRepository.getMaterialWithProgress(materialId)
            if (item == null) {
                _uiState.update { it.copy(isLoading = false, materialWithProgress = null) }
                return@launch
            }
            if (item.status == MaterialStatus.LOCKED) {
                _uiState.update {
                    it.copy(isLoading = false, isLocked = true, materialWithProgress = item)
                }
                return@launch
            }
            progressRepository.markMaterialOpened(materialId)
            val updated = progressRepository.getMaterialWithProgress(materialId) ?: item
            val grammar = GrammarCatalog.findGrammarInMaterial(updated.material)
            val familiar = progressRepository.getFamiliarWords(minCompletedTexts = 2)

            val keyVocab = mutableListOf<DictionaryEntry>()
            val seenIds = mutableSetOf<String>()
            for (p in updated.material.paragraphs) {
                for (s in p.sentences) {
                    for (seg in s.segments) {
                        if (keyVocab.size >= 7) break
                        if (seg.reading.isNullOrBlank()) continue
                        val entry = contentRepository.lookupWord(seg.dictId, seg.text, seg.reading) ?: continue
                        if (!entry.partOfSpeech.startsWith("числительное") && seenIds.add(entry.id)) {
                            keyVocab += entry
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLocked = false,
                    materialWithProgress = updated,
                    grammarPoints = grammar,
                    keyVocabulary = keyVocab,
                    familiarWords = familiar
                )
            }
        }
    }

    fun onSegmentClick(segment: TextSegment) {
        viewModelScope.launch {
            val entry = contentRepository.lookupWord(segment.dictId, segment.text, segment.reading)
            _uiState.update {
                it.copy(
                    selectedWord = WordPopupState(
                        segment = segment,
                        entry = entry
                    )
                )
            }
        }
    }

    fun dismissWordPopup() {
        _uiState.update { it.copy(selectedWord = null) }
    }

    fun onGrammarClick(point: GrammarPoint) {
        _uiState.update { it.copy(selectedGrammar = point) }
    }

    fun dismissGrammarPopup() {
        _uiState.update { it.copy(selectedGrammar = null) }
    }

    fun onScrollProgressChanged(progress: Float) {
        viewModelScope.launch {
            progressRepository.updateScrollProgress(materialId, progress)
        }
    }

    fun updateFontSize(sizeSp: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(sizeSp) }
    }

    fun updateLineHeight(multiplier: Float) {
        viewModelScope.launch { settingsRepository.setLineHeightMultiplier(multiplier) }
    }

    fun updateShowFurigana(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowFurigana(show) }
    }

    fun updateProgressiveFurigana(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setProgressiveFurigana(enabled) }
    }

    fun updateDisplayMode(mode: TextDisplayMode) {
        viewModelScope.launch { settingsRepository.setDisplayMode(mode) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
