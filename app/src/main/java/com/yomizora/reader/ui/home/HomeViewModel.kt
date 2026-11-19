package com.yomizora.reader.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.LevelProgress
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.domain.model.StudyStats
import com.yomizora.reader.domain.model.UserProfile
import com.yomizora.reader.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val activeProfile: UserProfile? = null,
    val profiles: List<UserProfile> = emptyList(),
    val levelProgresses: List<LevelProgress> = listOf(
        LevelProgress(JlptLevel.N5, 0, 0, 0),
        LevelProgress(JlptLevel.N4, 0, 0, 0)
    ),
    val continueMaterial: MaterialWithProgress? = null,
    val studyStats: StudyStats = StudyStats(0, 0, 0),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        progressRepository.observeActiveProfile(),
        progressRepository.observeProfiles(),
        combine(JlptLevel.entries.map { progressRepository.observeLevelProgress(it) }) { it.toList() },
        progressRepository.observeContinueLearningMaterial(),
        progressRepository.observeStudyStats()
    ) { profile, profiles, progresses, continueMat, stats ->
        val visibleProgresses = progresses.filter {
            it.level == JlptLevel.N5 || it.level == JlptLevel.N4 || it.totalCount > 0
        }
        HomeUiState(
            activeProfile = profile,
            profiles = profiles,
            levelProgresses = visibleProgresses,
            continueMaterial = continueMat,
            studyStats = stats,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            progressRepository.switchProfile(profileId)
        }
    }
}
