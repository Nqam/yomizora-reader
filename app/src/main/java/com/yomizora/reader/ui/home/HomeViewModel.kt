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
    val n5Progress: LevelProgress = LevelProgress(JlptLevel.N5, 0, 0, 0),
    val n4Progress: LevelProgress = LevelProgress(JlptLevel.N4, 0, 0, 0),
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
        progressRepository.observeLevelProgress(JlptLevel.N5),
        progressRepository.observeLevelProgress(JlptLevel.N4),
        progressRepository.observeContinueLearningMaterial(),
        progressRepository.observeStudyStats()
    ) { values ->
        val profile = values[0] as UserProfile?
        @Suppress("UNCHECKED_CAST")
        val profiles = values[1] as List<UserProfile>
        val n5 = values[2] as LevelProgress
        val n4 = values[3] as LevelProgress
        val continueMat = values[4] as MaterialWithProgress?
        val stats = values[5] as StudyStats
        HomeUiState(
            activeProfile = profile,
            profiles = profiles,
            n5Progress = n5,
            n4Progress = n4,
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
