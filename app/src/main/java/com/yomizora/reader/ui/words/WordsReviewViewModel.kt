package com.yomizora.reader.ui.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.domain.model.WordReviewItem
import com.yomizora.reader.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordQuizCard(
    val item: WordReviewItem,
    val options: List<String>,
    val correctIndex: Int
)

data class WordsReviewUiState(
    val isLoading: Boolean = true,
    val totalWordsCount: Int = 0,
    val dueWordsCount: Int = 0,
    val masteredCount: Int = 0,
    val masteredWords: List<WordReviewItem> = emptyList(),
    val nextDueInHours: Long? = null,
    val isFreePracticeMode: Boolean = false,
    val freePracticeRemaining: Int = 0,
    val currentCard: WordQuizCard? = null,
    val selectedOptionIndex: Int? = null,
    val isChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val scheduledDays: Int = 0
)

@HiltViewModel
class WordsReviewViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WordsReviewUiState())
    val uiState: StateFlow<WordsReviewUiState> = _uiState.asStateFlow()

    private var allWords: List<WordReviewItem> = emptyList()
    private val freePracticeQueue = ArrayDeque<WordReviewItem>()

    init {
        refreshDeck()
        viewModelScope.launch {
            progressRepository.observeWordReviews().collect { list ->
                allWords = list
                rebuildState()
            }
        }
    }

    fun refreshDeck() {
        viewModelScope.launch {
            progressRepository.syncWordsFromCompletedMaterials()
        }
    }

    fun startFreePracticeAll() {
        if (allWords.isEmpty()) return
        freePracticeQueue.clear()
        val unmastered = allWords.filter { it.repetitions < 3 }.shuffled()
        val masteredSample = allWords.filter { it.repetitions >= 3 }.shuffled()
        if (unmastered.isEmpty()) {
            freePracticeQueue.addAll(masteredSample)
        } else {
            // Unmastered words appear first; mastered words appear much less frequently (1 per 4 unmastered)
            val mIter = masteredSample.iterator()
            unmastered.forEachIndexed { idx, w ->
                freePracticeQueue.addLast(w)
                if ((idx + 1) % 4 == 0 && mIter.hasNext()) {
                    freePracticeQueue.addLast(mIter.next())
                }
            }
        }
        val first = freePracticeQueue.removeFirstOrNull()
        _uiState.update {
            it.copy(
                isFreePracticeMode = true,
                freePracticeRemaining = freePracticeQueue.size + if (first != null) 1 else 0,
                currentCard = first?.let { w -> buildQuizCard(w, allWords) },
                selectedOptionIndex = null,
                isChecked = false,
                isCorrect = false,
                scheduledDays = 0
            )
        }
    }

    fun exitFreePractice() {
        freePracticeQueue.clear()
        _uiState.update {
            it.copy(
                isFreePracticeMode = false,
                freePracticeRemaining = 0,
                selectedOptionIndex = null,
                isChecked = false,
                isCorrect = false
            )
        }
        rebuildState()
    }

    private fun rebuildState() {
        val now = System.currentTimeMillis()
        val due = allWords.filter { it.nextReviewAt <= now }
            .sortedWith(compareBy<WordReviewItem> { it.repetitions >= 3 }.thenBy { it.nextReviewAt })
        val masteredList = allWords.filter { it.repetitions >= 3 }
            .sortedWith(compareByDescending<WordReviewItem> { it.repetitions }.thenBy { it.word })
        val mastered = masteredList.size
        val nextUpcoming = allWords
            .filter { it.nextReviewAt > now }
            .minByOrNull { it.nextReviewAt }
        val hoursLeft = nextUpcoming?.let {
            ((it.nextReviewAt - now) / (60L * 60L * 1000L)).coerceAtLeast(1L)
        }

        val state = _uiState.value
        val cardToShow = when {
            state.isFreePracticeMode -> state.currentCard
            state.currentCard != null && state.isChecked -> state.currentCard
            else -> due.firstOrNull()?.let { buildQuizCard(it, allWords) }
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                totalWordsCount = allWords.size,
                dueWordsCount = due.size,
                masteredCount = mastered,
                masteredWords = masteredList,
                nextDueInHours = hoursLeft,
                currentCard = cardToShow
            )
        }
    }

    private fun buildQuizCard(target: WordReviewItem, pool: List<WordReviewItem>): WordQuizCard {
        val distractors = pool
            .asSequence()
            .filter { it.wordId != target.wordId && it.translationRu != target.translationRu }
            .map { it.translationRu }
            .distinct()
            .shuffled()
            .take(3)
            .toMutableList()

        val fallbackDistractors = listOf("время", "человек", "место", "действие", "дорога", "книга")
        for (fb in fallbackDistractors) {
            if (distractors.size >= 3) break
            if (fb != target.translationRu && fb !in distractors) {
                distractors += fb
            }
        }

        val options = (distractors + target.translationRu).shuffled()
        val correctIdx = options.indexOf(target.translationRu)
        return WordQuizCard(
            item = target,
            options = options,
            correctIndex = correctIdx
        )
    }

    fun selectAndCheckOption(index: Int) {
        val state = _uiState.value
        val card = state.currentCard ?: return
        if (state.isChecked) return

        val correct = index == card.correctIndex
        if (state.isFreePracticeMode) {
            if (!correct) {
                freePracticeQueue.addLast(card.item)
            }
            _uiState.update {
                it.copy(
                    selectedOptionIndex = index,
                    isChecked = true,
                    isCorrect = correct,
                    scheduledDays = 0
                )
            }
        } else {
            viewModelScope.launch {
                val days = progressRepository.recordWordReviewResult(card.item.wordId, correct)
                _uiState.update {
                    it.copy(
                        selectedOptionIndex = index,
                        isChecked = true,
                        isCorrect = correct,
                        scheduledDays = days
                    )
                }
            }
        }
    }

    fun nextCard() {
        val state = _uiState.value
        if (state.isFreePracticeMode) {
            val nextItem = freePracticeQueue.removeFirstOrNull()
            if (nextItem == null) {
                exitFreePractice()
                return
            }
            _uiState.update {
                it.copy(
                    freePracticeRemaining = freePracticeQueue.size + 1,
                    currentCard = buildQuizCard(nextItem, allWords),
                    selectedOptionIndex = null,
                    isChecked = false,
                    isCorrect = false,
                    scheduledDays = 0
                )
            }
        } else {
            val now = System.currentTimeMillis()
            val due = allWords.filter { it.nextReviewAt <= now }
            val nextCard = due.firstOrNull()?.let { buildQuizCard(it, allWords) }
            _uiState.update {
                it.copy(
                    currentCard = nextCard,
                    selectedOptionIndex = null,
                    isChecked = false,
                    isCorrect = false,
                    scheduledDays = 0
                )
            }
        }
    }
}
