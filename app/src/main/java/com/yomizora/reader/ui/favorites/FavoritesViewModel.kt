package com.yomizora.reader.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.domain.model.SavedWord
import com.yomizora.reader.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val favoriteMaterials: List<MaterialWithProgress> = emptyList(),
    val savedWords: List<SavedWord> = emptyList()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = combine(
        progressRepository.observeFavoriteMaterials(),
        progressRepository.observeSavedWords()
    ) { materials, words ->
        FavoritesUiState(
            favoriteMaterials = materials,
            savedWords = words
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavoritesUiState()
    )

    fun removeFavoriteMaterial(materialId: String) {
        viewModelScope.launch {
            progressRepository.toggleFavoriteMaterial(materialId)
        }
    }

    fun removeSavedWord(wordId: String) {
        viewModelScope.launch {
            progressRepository.removeSavedWord(wordId)
        }
    }
}
