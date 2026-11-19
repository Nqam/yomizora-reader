package com.yomizora.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LibraryUiState(
    val selectedLevel: JlptLevel = JlptLevel.N5,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val availableCategories: List<String> = emptyList(),
    val items: List<MaterialWithProgress> = emptyList(),
    val totalInLevel: Int = 0,
    val completedInLevel: Int = 0,
    val isListView: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _selectedLevel = MutableStateFlow(JlptLevel.N5)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _isListView = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val levelMaterialsFlow = _selectedLevel.flatMapLatest { level ->
        progressRepository.observeMaterialsWithProgress(level)
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedLevel,
        _searchQuery,
        _selectedCategory,
        _isListView,
        levelMaterialsFlow
    ) { level, query, category, isListView, materials ->
        val categories = materials.map { it.material.category }.distinct().sorted()
        val trimmedQuery = query.trim().lowercase()
        val filtered = materials.filter { item ->
            val m = item.material
            val matchesCategory = category == null || m.category == category
            val matchesQuery = trimmedQuery.isEmpty() ||
                m.title.lowercase().contains(trimmedQuery) ||
                m.description.lowercase().contains(trimmedQuery) ||
                m.category.lowercase().contains(trimmedQuery) ||
                m.level.code.lowercase().contains(trimmedQuery) ||
                m.plainText().lowercase().contains(trimmedQuery) ||
                m.orderIndex.toString().padStart(2, '0').contains(trimmedQuery)
            matchesCategory && matchesQuery
        }
        LibraryUiState(
            selectedLevel = level,
            searchQuery = query,
            selectedCategory = category,
            availableCategories = categories,
            items = filtered,
            totalInLevel = materials.size,
            completedInLevel = materials.count {
                it.status == com.yomizora.reader.domain.model.MaterialStatus.COMPLETED
            },
            isListView = isListView
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    fun toggleViewMode() {
        _isListView.value = !_isListView.value
    }

    fun selectLevel(level: JlptLevel) {
        if (_selectedLevel.value != level) {
            _selectedLevel.value = level
            _selectedCategory.value = null
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }
}
