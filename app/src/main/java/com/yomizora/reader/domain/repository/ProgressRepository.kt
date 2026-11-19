package com.yomizora.reader.domain.repository

import com.yomizora.reader.domain.model.DictionaryEntry
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.LevelProgress
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.domain.model.SavedWord
import com.yomizora.reader.domain.model.StudyStats
import com.yomizora.reader.domain.model.UserProfile
import com.yomizora.reader.domain.model.WordReviewItem
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun observeProfiles(): Flow<List<UserProfile>>
    fun observeActiveProfile(): Flow<UserProfile>
    suspend fun createProfile(name: String): UserProfile
    suspend fun switchProfile(profileId: String)
    suspend fun renameProfile(profileId: String, newName: String)
    suspend fun deleteProfile(profileId: String)

    fun observeMaterialsWithProgress(level: JlptLevel): Flow<List<MaterialWithProgress>>
    fun observeLevelProgress(level: JlptLevel): Flow<LevelProgress>
    fun observeFavoriteMaterials(): Flow<List<MaterialWithProgress>>
    fun observeSavedWords(): Flow<List<SavedWord>>
    fun observeStudyStats(): Flow<StudyStats>
    fun observeContinueLearningMaterial(): Flow<MaterialWithProgress?>

    suspend fun getMaterialWithProgress(materialId: String): MaterialWithProgress?
    suspend fun markMaterialOpened(materialId: String)
    suspend fun updateScrollProgress(materialId: String, progress: Float)
    suspend fun markMaterialCompleted(materialId: String, answers: List<Int>)
    suspend fun toggleFavoriteMaterial(materialId: String)
    suspend fun toggleSavedWord(entry: DictionaryEntry)
    suspend fun isWordSaved(wordId: String): Boolean
    suspend fun removeSavedWord(wordId: String)
    suspend fun resetLevelProgress(level: JlptLevel)
    suspend fun exportProgressBackupJson(): String
    suspend fun importProgressBackupJson(rawJson: String): Int
    suspend fun getFamiliarWords(minCompletedTexts: Int = 2): Set<Pair<String, String>>
    suspend fun getSeenGrammarIds(excludeMaterialId: String? = null): Set<String>

    fun observeWordReviews(): Flow<List<WordReviewItem>>
    suspend fun syncWordsFromCompletedMaterials()
    suspend fun recordWordReviewResult(wordId: String, isCorrect: Boolean): Int
}
