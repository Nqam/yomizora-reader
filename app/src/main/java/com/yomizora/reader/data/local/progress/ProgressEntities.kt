package com.yomizora.reader.data.local.progress

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "material_progress",
    primaryKeys = ["profileId", "materialId"],
    indices = [
        Index(value = ["profileId", "level"]),
        Index(value = ["profileId", "isFavorite"])
    ]
)
data class MaterialProgressEntity(
    val profileId: String,
    val materialId: String,
    val level: String,
    val state: String, // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val isFavorite: Boolean = false,
    val lastOpenedAt: Long? = null,
    val completedAt: Long? = null,
    val lastScrollProgress: Float = 0f,
    val attemptsCount: Int = 0,
    val lastAnswersJson: String? = null
)

@Entity(
    tableName = "saved_words",
    primaryKeys = ["profileId", "wordId"]
)
data class SavedWordEntity(
    val profileId: String,
    val wordId: String,
    val word: String,
    val reading: String,
    val translationRu: String,
    val partOfSpeech: String,
    val note: String?,
    val savedAt: Long
)

@Entity(
    tableName = "study_activity",
    primaryKeys = ["profileId", "dateIso"]
)
data class StudyActivityEntity(
    val profileId: String,
    val dateIso: String, // yyyy-MM-dd
    val textsOpened: Int = 0,
    val textsCompleted: Int = 0
)

@Entity(tableName = "user_state")
data class UserStateEntity(
    @PrimaryKey val profileId: String,
    val lastOpenedMaterialId: String? = null,
    val lastOpenedLevel: String? = null
)

@Entity(
    tableName = "word_reviews",
    primaryKeys = ["profileId", "wordId"],
    indices = [Index(value = ["profileId", "nextReviewAt"])]
)
data class WordReviewEntity(
    val profileId: String,
    val wordId: String,
    val word: String,
    val reading: String,
    val translationRu: String,
    val partOfSpeech: String,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val nextReviewAt: Long = 0L,
    val lastReviewedAt: Long? = null
)
