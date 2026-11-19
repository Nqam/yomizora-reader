package com.yomizora.reader.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.domain.model.MaterialStatus
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.ReadingMaterial
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

data class QuizUiState(
    val isLoading: Boolean = true,
    val isLocked: Boolean = false,
    val material: ReadingMaterial? = null,
    val familiarWords: Set<Pair<String, String>> = emptySet(),
    val currentQuestionIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerChecked: Boolean = false,
    val isCurrentAnswerCorrect: Boolean = false,
    val recordedAnswers: List<Int> = emptyList(),
    val isQuizCompleted: Boolean = false,
    val nextMaterial: ReadingMaterial? = null
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val progressRepository: ProgressRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val materialId: String = checkNotNull(savedStateHandle["materialId"])

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    val settings: StateFlow<ReaderSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReaderSettings()
    )

    init {
        loadQuiz()
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            val item = progressRepository.getMaterialWithProgress(materialId)
            if (item == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            if (item.status == MaterialStatus.LOCKED) {
                _uiState.update { it.copy(isLoading = false, isLocked = true) }
                return@launch
            }
            val next = contentRepository.getNextMaterialInLevel(
                item.material.level,
                item.material.orderIndex
            )
            val familiar = progressRepository.getFamiliarWords(minCompletedTexts = 2)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLocked = false,
                    material = item.material,
                    familiarWords = familiar,
                    nextMaterial = next
                )
            }
        }
    }

    fun selectOption(index: Int) {
        val state = _uiState.value
        if (state.isAnswerChecked && state.isCurrentAnswerCorrect) return
        _uiState.update {
            it.copy(
                selectedOptionIndex = index,
                isAnswerChecked = false
            )
        }
    }

    fun checkAnswer() {
        val state = _uiState.value
        val material = state.material ?: return
        val selected = state.selectedOptionIndex ?: return
        val question = material.questions.getOrNull(state.currentQuestionIndex) ?: return

        val isCorrect = selected == question.correctAnswer
        _uiState.update {
            it.copy(
                isAnswerChecked = true,
                isCurrentAnswerCorrect = isCorrect
            )
        }
    }

    fun retryCurrentQuestion() {
        _uiState.update {
            it.copy(
                selectedOptionIndex = null,
                isAnswerChecked = false,
                isCurrentAnswerCorrect = false
            )
        }
    }

    fun advanceOrFinish() {
        val state = _uiState.value
        val material = state.material ?: return
        val selected = state.selectedOptionIndex ?: return
        if (!state.isCurrentAnswerCorrect) return

        val updatedAnswers = state.recordedAnswers + selected
        val nextIndex = state.currentQuestionIndex + 1

        if (nextIndex < material.questions.size) {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    selectedOptionIndex = null,
                    isAnswerChecked = false,
                    isCurrentAnswerCorrect = false,
                    recordedAnswers = updatedAnswers
                )
            }
        } else {
            viewModelScope.launch {
                progressRepository.markMaterialCompleted(material.id, updatedAnswers)
                _uiState.update {
                    it.copy(
                        recordedAnswers = updatedAnswers,
                        isQuizCompleted = true
                    )
                }
            }
        }
    }
}
