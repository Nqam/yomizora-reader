package com.yomizora.reader.data.local.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("UPDATE profiles SET name = :newName WHERE id = :id")
    suspend fun rename(id: String, newName: String)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface MaterialProgressDao {
    @Query("SELECT * FROM material_progress WHERE profileId = :profileId AND level = :level")
    fun observeByLevel(profileId: String, level: String): Flow<List<MaterialProgressEntity>>

    @Query("SELECT * FROM material_progress WHERE profileId = :profileId AND isFavorite = 1 ORDER BY lastOpenedAt DESC")
    fun observeFavorites(profileId: String): Flow<List<MaterialProgressEntity>>

    @Query("SELECT * FROM material_progress WHERE profileId = :profileId")
    fun observeAll(profileId: String): Flow<List<MaterialProgressEntity>>

    @Query("SELECT * FROM material_progress WHERE profileId = :profileId")
    suspend fun getAllOnce(profileId: String): List<MaterialProgressEntity>

    @Query("SELECT * FROM material_progress WHERE profileId = :profileId AND materialId = :materialId LIMIT 1")
    suspend fun getById(profileId: String, materialId: String): MaterialProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MaterialProgressEntity)

    @Query(
        """
        UPDATE material_progress
        SET state = 'NOT_STARTED',
            lastOpenedAt = NULL,
            completedAt = NULL,
            lastScrollProgress = 0.0,
            attemptsCount = 0,
            lastAnswersJson = NULL
        WHERE profileId = :profileId AND level = :level
        """
    )
    suspend fun resetProgressKeepFavorites(profileId: String, level: String)

    @Query("DELETE FROM material_progress WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}

@Dao
interface SavedWordDao {
    @Query("SELECT * FROM saved_words WHERE profileId = :profileId ORDER BY savedAt DESC")
    fun observeAll(profileId: String): Flow<List<SavedWordEntity>>

    @Query("SELECT * FROM saved_words WHERE profileId = :profileId")
    suspend fun getAllOnce(profileId: String): List<SavedWordEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_words WHERE profileId = :profileId AND wordId = :wordId)")
    suspend fun exists(profileId: String, wordId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedWordEntity)

    @Query("DELETE FROM saved_words WHERE profileId = :profileId AND wordId = :wordId")
    suspend fun deleteById(profileId: String, wordId: String)

    @Query("DELETE FROM saved_words WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}

@Dao
interface StudyActivityDao {
    @Query("SELECT * FROM study_activity WHERE profileId = :profileId ORDER BY dateIso DESC")
    fun observeAll(profileId: String): Flow<List<StudyActivityEntity>>

    @Query("SELECT * FROM study_activity WHERE profileId = :profileId AND dateIso = :dateIso LIMIT 1")
    suspend fun getByDate(profileId: String, dateIso: String): StudyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StudyActivityEntity)

    @Query("DELETE FROM study_activity WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}

@Dao
interface UserStateDao {
    @Query("SELECT * FROM user_state WHERE profileId = :profileId LIMIT 1")
    fun observe(profileId: String): Flow<UserStateEntity?>

    @Query("SELECT * FROM user_state WHERE profileId = :profileId LIMIT 1")
    suspend fun getOnce(profileId: String): UserStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: UserStateEntity)

    @Query("DELETE FROM user_state WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}

@Dao
interface WordReviewDao {
    @Query("SELECT * FROM word_reviews WHERE profileId = :profileId ORDER BY nextReviewAt ASC")
    fun observeAll(profileId: String): Flow<List<WordReviewEntity>>

    @Query("SELECT * FROM word_reviews WHERE profileId = :profileId")
    suspend fun getAllOnce(profileId: String): List<WordReviewEntity>

    @Query("SELECT * FROM word_reviews WHERE profileId = :profileId AND wordId = :wordId LIMIT 1")
    suspend fun getById(profileId: String, wordId: String): WordReviewEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(entries: List<WordReviewEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WordReviewEntity)

    @Query("DELETE FROM word_reviews WHERE profileId = :profileId")
    suspend fun deleteAllForProfile(profileId: String)
}
