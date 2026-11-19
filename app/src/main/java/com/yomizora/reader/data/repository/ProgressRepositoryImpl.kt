package com.yomizora.reader.data.repository

import androidx.room.withTransaction
import com.yomizora.reader.data.local.progress.MaterialProgressEntity
import com.yomizora.reader.data.local.progress.ProfileEntity
import com.yomizora.reader.data.local.progress.SavedWordEntity
import com.yomizora.reader.data.local.progress.StudyActivityEntity
import com.yomizora.reader.data.local.progress.UserProgressDatabase
import com.yomizora.reader.data.local.progress.UserStateEntity
import com.yomizora.reader.data.local.progress.WordReviewEntity
import com.yomizora.reader.domain.model.DictionaryEntry
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.LevelProgress
import com.yomizora.reader.domain.model.MaterialStatus
import com.yomizora.reader.domain.model.MaterialWithProgress
import com.yomizora.reader.domain.model.ReadingMaterial
import com.yomizora.reader.domain.model.SavedWord
import com.yomizora.reader.domain.model.StudyStats
import com.yomizora.reader.domain.model.UserProfile
import com.yomizora.reader.domain.model.WordReviewItem
import com.yomizora.reader.domain.repository.ContentRepository
import com.yomizora.reader.domain.repository.ProgressRepository
import com.yomizora.reader.domain.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProgressRepositoryImpl @Inject constructor(
    private val contentRepository: ContentRepository,
    private val progressDatabase: UserProgressDatabase,
    private val settingsRepository: SettingsRepository,
    private val json: Json
) : ProgressRepository {

    private val profileDao = progressDatabase.profileDao()
    private val progressDao = progressDatabase.materialProgressDao()
    private val savedWordDao = progressDatabase.savedWordDao()
    private val activityDao = progressDatabase.studyActivityDao()
    private val userStateDao = progressDatabase.userStateDao()
    private val wordReviewDao = progressDatabase.wordReviewDao()

    private suspend fun ensureDefaultProfile() {
        if (profileDao.getAllOnce().isEmpty()) {
            profileDao.upsert(
                ProfileEntity(
                    id = DEFAULT_PROFILE_ID,
                    name = DEFAULT_PROFILE_NAME,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun currentProfileId(): String {
        ensureDefaultProfile()
        val id = settingsRepository.activeProfileIdFlow.first()
        return if (profileDao.getById(id) != null) id else {
            val fallback = profileDao.getAllOnce().first().id
            settingsRepository.setActiveProfileId(fallback)
            fallback
        }
    }

    override fun observeProfiles(): Flow<List<UserProfile>> =
        profileDao.observeAll()
            .onStart { ensureDefaultProfile() }
            .map { list ->
                list.map { UserProfile(id = it.id, name = it.name, createdAt = it.createdAt) }
            }

    override fun observeActiveProfile(): Flow<UserProfile> =
        combine(
            observeProfiles(),
            settingsRepository.activeProfileIdFlow
        ) { profiles, activeId ->
            profiles.firstOrNull { it.id == activeId }
                ?: profiles.firstOrNull()
                ?: UserProfile(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME, 0L)
        }

    override suspend fun createProfile(name: String): UserProfile {
        ensureDefaultProfile()
        val trimmed = name.trim().ifBlank { "Новый профиль" }
        val entity = ProfileEntity(
            id = "p_${UUID.randomUUID().toString().take(8)}",
            name = trimmed,
            createdAt = System.currentTimeMillis()
        )
        profileDao.upsert(entity)
        settingsRepository.setActiveProfileId(entity.id)
        return UserProfile(entity.id, entity.name, entity.createdAt)
    }

    override suspend fun switchProfile(profileId: String) {
        if (profileDao.getById(profileId) != null) {
            settingsRepository.setActiveProfileId(profileId)
        }
    }

    override suspend fun renameProfile(profileId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isNotBlank()) {
            profileDao.rename(profileId, trimmed)
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        val all = profileDao.getAllOnce()
        if (all.size <= 1) return // Keep at least one profile
        val activeId = settingsRepository.activeProfileIdFlow.first()
        if (activeId == profileId) {
            val next = all.first { it.id != profileId }
            settingsRepository.setActiveProfileId(next.id)
        }
        progressDatabase.withTransaction {
            progressDao.deleteAllForProfile(profileId)
            savedWordDao.deleteAllForProfile(profileId)
            activityDao.deleteAllForProfile(profileId)
            userStateDao.deleteForProfile(profileId)
            wordReviewDao.deleteAllForProfile(profileId)
            profileDao.deleteById(profileId)
        }
    }

    override fun observeMaterialsWithProgress(level: JlptLevel): Flow<List<MaterialWithProgress>> =
        settingsRepository.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                contentRepository.observeMaterialsByLevel(level),
                progressDao.observeByLevel(profileId, level.code)
            ) { materials, progressList ->
                computeStatuses(materials, progressList.associateBy { it.materialId })
            }
        }

    override fun observeLevelProgress(level: JlptLevel): Flow<LevelProgress> =
        observeMaterialsWithProgress(level).map { list ->
            LevelProgress(
                level = level,
                totalCount = list.size,
                completedCount = list.count { it.status == MaterialStatus.COMPLETED },
                inProgressCount = list.count { it.status == MaterialStatus.IN_PROGRESS }
            )
        }

    override fun observeFavoriteMaterials(): Flow<List<MaterialWithProgress>> =
        settingsRepository.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                contentRepository.observeAllMaterials(),
                progressDao.observeAll(profileId)
            ) { allMaterials, allProgress ->
                val progressMap = allProgress.associateBy { it.materialId }
                val byLevel = allMaterials.groupBy { it.level }
                byLevel.values
                    .flatMap { levelMaterials -> computeStatuses(levelMaterials, progressMap) }
                    .filter { it.isFavorite }
            }
        }

    override fun observeSavedWords(): Flow<List<SavedWord>> =
        settingsRepository.activeProfileIdFlow.flatMapLatest { profileId ->
            savedWordDao.observeAll(profileId).map { entities ->
                entities.map {
                    SavedWord(
                        wordId = it.wordId,
                        word = it.word,
                        reading = it.reading,
                        translationRu = it.translationRu,
                        partOfSpeech = it.partOfSpeech,
                        note = it.note,
                        savedAt = it.savedAt
                    )
                }
            }
        }

    override fun observeStudyStats(): Flow<StudyStats> =
        settingsRepository.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                activityDao.observeAll(profileId),
                progressDao.observeAll(profileId)
            ) { activities, allProgress ->
                val activeDates = activities
                    .filter { it.textsOpened > 0 || it.textsCompleted > 0 }
                    .mapNotNull { runCatching { LocalDate.parse(it.dateIso) }.getOrNull() }
                    .toSet()

                val streak = calculateStreak(activeDates, LocalDate.now())
                val totalCompleted = allProgress.count { it.state == STATE_COMPLETED }
                StudyStats(
                    studyDays = activeDates.size,
                    currentStreak = streak,
                    totalCompleted = totalCompleted
                )
            }
        }

    override fun observeContinueLearningMaterial(): Flow<MaterialWithProgress?> =
        settingsRepository.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                userStateDao.observe(profileId),
                observeMaterialsWithProgress(JlptLevel.N5),
                observeMaterialsWithProgress(JlptLevel.N4)
            ) { userState, n5List, n4List ->
                val all = n5List + n4List
                val lastId = userState?.lastOpenedMaterialId
                val lastMatch = if (lastId != null) {
                    all.firstOrNull { it.material.id == lastId && it.status != MaterialStatus.LOCKED }
                } else null

                lastMatch
                    ?: all.firstOrNull { it.status == MaterialStatus.IN_PROGRESS }
                    ?: all.firstOrNull { it.status == MaterialStatus.AVAILABLE }
                    ?: all.firstOrNull { it.status != MaterialStatus.LOCKED }
            }
        }

    override suspend fun getMaterialWithProgress(materialId: String): MaterialWithProgress? {
        val material = contentRepository.getMaterialById(materialId) ?: return null
        val list = observeMaterialsWithProgress(material.level).first()
        return list.firstOrNull { it.material.id == materialId }
    }

    override suspend fun markMaterialOpened(materialId: String) {
        val material = contentRepository.getMaterialById(materialId) ?: return
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        progressDatabase.withTransaction {
            val existing = progressDao.getById(profileId, materialId)
            val newState = when (existing?.state) {
                STATE_COMPLETED -> STATE_COMPLETED
                else -> STATE_IN_PROGRESS
            }
            progressDao.upsert(
                MaterialProgressEntity(
                    profileId = profileId,
                    materialId = materialId,
                    level = material.level.code,
                    state = newState,
                    isFavorite = existing?.isFavorite ?: false,
                    lastOpenedAt = now,
                    completedAt = existing?.completedAt,
                    lastScrollProgress = existing?.lastScrollProgress ?: 0f,
                    attemptsCount = existing?.attemptsCount ?: 0,
                    lastAnswersJson = existing?.lastAnswersJson
                )
            )
            userStateDao.upsert(
                UserStateEntity(
                    profileId = profileId,
                    lastOpenedMaterialId = materialId,
                    lastOpenedLevel = material.level.code
                )
            )
            recordActivity(profileId, openedDelta = 1, completedDelta = 0)
        }
    }

    override suspend fun updateScrollProgress(materialId: String, progress: Float) {
        val profileId = currentProfileId()
        val existing = progressDao.getById(profileId, materialId) ?: return
        val clamped = progress.coerceIn(0f, 1f)
        if (clamped > existing.lastScrollProgress) {
            progressDao.upsert(existing.copy(lastScrollProgress = clamped))
        }
    }

    override suspend fun markMaterialCompleted(materialId: String, answers: List<Int>) {
        val material = contentRepository.getMaterialById(materialId) ?: return
        val profileId = currentProfileId()
        val now = System.currentTimeMillis()
        val answersJson = json.encodeToString(answers)
        progressDatabase.withTransaction {
            val existing = progressDao.getById(profileId, materialId)
            val wasAlreadyCompleted = existing?.state == STATE_COMPLETED
            progressDao.upsert(
                MaterialProgressEntity(
                    profileId = profileId,
                    materialId = materialId,
                    level = material.level.code,
                    state = STATE_COMPLETED,
                    isFavorite = existing?.isFavorite ?: false,
                    lastOpenedAt = now,
                    completedAt = existing?.completedAt ?: now,
                    lastScrollProgress = 1f,
                    attemptsCount = (existing?.attemptsCount ?: 0) + 1,
                    lastAnswersJson = answersJson
                )
            )
            if (!wasAlreadyCompleted) {
                recordActivity(profileId, openedDelta = 0, completedDelta = 1)
            }
        }
    }

    override suspend fun toggleFavoriteMaterial(materialId: String) {
        val material = contentRepository.getMaterialById(materialId) ?: return
        val profileId = currentProfileId()
        progressDatabase.withTransaction {
            val existing = progressDao.getById(profileId, materialId)
            if (existing != null) {
                progressDao.upsert(existing.copy(isFavorite = !existing.isFavorite))
            } else {
                progressDao.upsert(
                    MaterialProgressEntity(
                        profileId = profileId,
                        materialId = materialId,
                        level = material.level.code,
                        state = STATE_NOT_STARTED,
                        isFavorite = true
                    )
                )
            }
        }
    }

    override suspend fun toggleSavedWord(entry: DictionaryEntry) {
        val profileId = currentProfileId()
        if (savedWordDao.exists(profileId, entry.id)) {
            savedWordDao.deleteById(profileId, entry.id)
        } else {
            savedWordDao.upsert(
                SavedWordEntity(
                    profileId = profileId,
                    wordId = entry.id,
                    word = entry.word,
                    reading = entry.reading,
                    translationRu = entry.translationRu,
                    partOfSpeech = entry.partOfSpeech,
                    note = entry.note,
                    savedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun isWordSaved(wordId: String): Boolean {
        val profileId = currentProfileId()
        return savedWordDao.exists(profileId, wordId)
    }

    override suspend fun removeSavedWord(wordId: String) {
        val profileId = currentProfileId()
        savedWordDao.deleteById(profileId, wordId)
    }

    override suspend fun resetLevelProgress(level: JlptLevel) {
        val profileId = currentProfileId()
        progressDatabase.withTransaction {
            progressDao.resetProgressKeepFavorites(profileId, level.code)
            val userState = userStateDao.getOnce(profileId)
            if (userState?.lastOpenedLevel == level.code) {
                userStateDao.upsert(userState.copy(lastOpenedMaterialId = null, lastOpenedLevel = null))
            }
            wordReviewDao.deleteAllForProfile(profileId)
        }
        // Re-populate words only from any remaining completed materials in other levels
        syncWordsFromCompletedMaterials()
    }

    override suspend fun exportProgressBackupJson(): String {
        val profileId = currentProfileId()
        val profile = profileDao.getById(profileId)
        val progress = progressDao.getAllOnce(profileId).map {
            BackupMaterialProgress(
                materialId = it.materialId,
                level = it.level,
                state = it.state,
                isFavorite = it.isFavorite,
                completedAt = it.completedAt
            )
        }
        val words = savedWordDao.getAllOnce(profileId).map { it.wordId }
        return json.encodeToString(
            ProgressBackupDto(
                profileName = profile?.name ?: DEFAULT_PROFILE_NAME,
                exportedAt = System.currentTimeMillis(),
                materials = progress,
                savedWordIds = words
            )
        )
    }

    override suspend fun importProgressBackupJson(rawJson: String): Int {
        val backup = json.decodeFromString<ProgressBackupDto>(rawJson)
        val profileId = currentProfileId()
        var restoredCount = 0
        progressDatabase.withTransaction {
            for (item in backup.materials) {
                if (item.state == STATE_COMPLETED || item.state == STATE_IN_PROGRESS || item.isFavorite) {
                    progressDao.upsert(
                        MaterialProgressEntity(
                            profileId = profileId,
                            materialId = item.materialId,
                            level = item.level,
                            state = item.state,
                            isFavorite = item.isFavorite,
                            lastOpenedAt = item.completedAt ?: System.currentTimeMillis(),
                            completedAt = item.completedAt,
                            lastScrollProgress = if (item.state == STATE_COMPLETED) 1f else 0f
                        )
                    )
                    restoredCount++
                }
            }
        }
        return restoredCount
    }

    override suspend fun getFamiliarWords(minCompletedTexts: Int): Set<Pair<String, String>> {
        val profileId = currentProfileId()
        val completedIds = progressDao.getAllOnce(profileId)
            .filter { it.state == STATE_COMPLETED }
            .map { it.materialId }
        if (completedIds.size < minCompletedTexts) return emptySet()

        val counts = mutableMapOf<Pair<String, String>, Int>()
        for (id in completedIds) {
            val mat = contentRepository.getMaterialById(id) ?: continue
            val uniqueWordsInMat = mat.paragraphs
                .flatMap { it.sentences }
                .flatMap { it.segments }
                .mapNotNull { seg ->
                    val r = seg.reading
                    if (!r.isNullOrBlank()) seg.text to r else null
                }
                .toSet()
            for (pair in uniqueWordsInMat) {
                counts[pair] = (counts[pair] ?: 0) + 1
            }
        }
        return counts.filterValues { it >= minCompletedTexts }.keys
    }

    override suspend fun getSeenGrammarIds(excludeMaterialId: String?): Set<String> {
        val profileId = currentProfileId()
        val completedIds = progressDao.getAllOnce(profileId)
            .filter { it.state == STATE_COMPLETED && it.materialId != excludeMaterialId }
            .map { it.materialId }
        if (completedIds.isEmpty()) return emptySet()

        val seen = mutableSetOf<String>()
        for (id in completedIds) {
            val mat = contentRepository.getMaterialById(id) ?: continue
            com.yomizora.reader.data.content.GrammarCatalog.findGrammarInMaterial(mat)
                .forEach { seen += it.id }
        }
        return seen
    }

    override fun observeWordReviews(): Flow<List<WordReviewItem>> =
        settingsRepository.activeProfileIdFlow.flatMapLatest { profileId ->
            wordReviewDao.observeAll(profileId).map { list ->
                list.map {
                    WordReviewItem(
                        wordId = it.wordId,
                        word = it.word,
                        reading = it.reading,
                        translationRu = it.translationRu,
                        partOfSpeech = it.partOfSpeech,
                        repetitions = it.repetitions,
                        intervalDays = it.intervalDays,
                        nextReviewAt = it.nextReviewAt,
                        lastReviewedAt = it.lastReviewedAt
                    )
                }
            }
        }

    override suspend fun syncWordsFromCompletedMaterials() {
        val profileId = currentProfileId()
        val completedIds = progressDao.getAllOnce(profileId)
            .filter { it.state == STATE_COMPLETED }
            .map { it.materialId }
        if (completedIds.isEmpty()) return

        val existingByWordId = wordReviewDao.getAllOnce(profileId).associateBy { it.wordId }
        val toInsert = mutableListOf<WordReviewEntity>()
        val toUpdate = mutableListOf<WordReviewEntity>()
        val seenIds = mutableSetOf<String>()
        for (id in completedIds) {
            val mat = contentRepository.getMaterialById(id) ?: continue
            for (p in mat.paragraphs) {
                for (s in p.sentences) {
                    for (seg in s.segments) {
                        if (seg.dictId.isNullOrBlank() && seg.reading.isNullOrBlank()) continue
                        val entry = contentRepository.lookupWord(seg.dictId, seg.text, seg.reading) ?: continue
                        if (!entry.partOfSpeech.startsWith("числительное") && seenIds.add(entry.id)) {
                            val existing = existingByWordId[entry.id]
                            if (existing == null) {
                                toInsert += WordReviewEntity(
                                    profileId = profileId,
                                    wordId = entry.id,
                                    word = entry.word,
                                    reading = entry.reading,
                                    translationRu = entry.translationRu,
                                    partOfSpeech = entry.partOfSpeech,
                                    repetitions = 0,
                                    intervalDays = 0,
                                    nextReviewAt = 0L,
                                    lastReviewedAt = null
                                )
                            } else if (existing.word != entry.word || existing.reading != entry.reading || existing.translationRu != entry.translationRu) {
                                toUpdate += existing.copy(
                                    word = entry.word,
                                    reading = entry.reading,
                                    translationRu = entry.translationRu,
                                    partOfSpeech = entry.partOfSpeech
                                )
                            }
                        }
                    }
                }
            }
        }
        if (toInsert.isNotEmpty()) {
            wordReviewDao.insertIgnoreAll(toInsert)
        }
        for (updated in toUpdate) {
            wordReviewDao.upsert(updated)
        }
    }

    override suspend fun recordWordReviewResult(wordId: String, isCorrect: Boolean): Int {
        val profileId = currentProfileId()
        val existing = wordReviewDao.getById(profileId, wordId) ?: return 0
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        val (newReps, newIntervalDays, nextReviewAt) = if (isCorrect) {
            val reps = existing.repetitions + 1
            val days = when (existing.repetitions) {
                0 -> 1
                1 -> 3
                2 -> 7
                else -> (existing.intervalDays * 2).coerceAtMost(60)
            }
            Triple(reps, days, now + days * dayMs)
        } else {
            Triple(0, 0, now)
        }

        wordReviewDao.upsert(
            existing.copy(
                repetitions = newReps,
                intervalDays = newIntervalDays,
                nextReviewAt = nextReviewAt,
                lastReviewedAt = now
            )
        )
        return newIntervalDays
    }

    private suspend fun recordActivity(profileId: String, openedDelta: Int, completedDelta: Int) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = activityDao.getByDate(profileId, today)
        activityDao.upsert(
            StudyActivityEntity(
                profileId = profileId,
                dateIso = today,
                textsOpened = (existing?.textsOpened ?: 0) + openedDelta,
                textsCompleted = (existing?.textsCompleted ?: 0) + completedDelta
            )
        )
    }

    private fun computeStatuses(
        materials: List<ReadingMaterial>,
        progressMap: Map<String, MaterialProgressEntity>
    ): List<MaterialWithProgress> {
        val sorted = materials.sortedBy { it.orderIndex }
        val result = ArrayList<MaterialWithProgress>(sorted.size)
        var previousCompleted = true

        for (material in sorted) {
            val entity = progressMap[material.id]
            val status = if (!previousCompleted) {
                MaterialStatus.LOCKED
            } else {
                when (entity?.state) {
                    STATE_COMPLETED -> MaterialStatus.COMPLETED
                    STATE_IN_PROGRESS -> MaterialStatus.IN_PROGRESS
                    else -> MaterialStatus.AVAILABLE
                }
            }
            previousCompleted = (status == MaterialStatus.COMPLETED)
            result += MaterialWithProgress(
                material = material,
                status = status,
                isFavorite = entity?.isFavorite ?: false,
                lastScrollProgress = entity?.lastScrollProgress ?: 0f,
                completedAt = entity?.completedAt
            )
        }
        return result
    }

    private fun calculateStreak(activeDates: Set<LocalDate>, today: LocalDate): Int {
        if (activeDates.isEmpty()) return 0
        var cursor = if (activeDates.contains(today)) today else today.minusDays(1)
        if (!activeDates.contains(cursor)) return 0
        var streak = 0
        while (activeDates.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    @Serializable
    private data class ProgressBackupDto(
        val profileName: String,
        val exportedAt: Long,
        val materials: List<BackupMaterialProgress>,
        val savedWordIds: List<String>
    )

    @Serializable
    private data class BackupMaterialProgress(
        val materialId: String,
        val level: String,
        val state: String,
        val isFavorite: Boolean,
        val completedAt: Long?
    )

    companion object {
        const val DEFAULT_PROFILE_ID = "default"
        const val DEFAULT_PROFILE_NAME = "Основной профиль"
        const val STATE_NOT_STARTED = "NOT_STARTED"
        const val STATE_IN_PROGRESS = "IN_PROGRESS"
        const val STATE_COMPLETED = "COMPLETED"
    }
}
