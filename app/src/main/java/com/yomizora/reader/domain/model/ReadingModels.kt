package com.yomizora.reader.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TextSegment(
    val text: String,
    val reading: String? = null,
    @SerialName("dict_id")
    val dictId: String? = null
)

@Serializable
data class Sentence(
    val segments: List<TextSegment>
) {
    fun plainText(): String = segments.joinToString(separator = "") { it.text }
}

@Serializable
data class Paragraph(
    val sentences: List<Sentence>
) {
    fun plainText(): String = sentences.joinToString(separator = "") { it.plainText() }
}

@Serializable
data class ComprehensionQuestion(
    val id: String,
    val type: String = "multiple_choice",
    val question: String,
    val options: List<String>,
    @SerialName("correct_answer")
    val correctAnswer: Int,
    val explanation: String,
    @SerialName("reference_excerpt")
    val referenceExcerpt: String? = null,
    @SerialName("question_segments")
    val questionSegments: List<TextSegment>? = null,
    @SerialName("option_segments")
    val optionSegments: List<List<TextSegment>>? = null
)

data class ReadingMaterial(
    val id: String,
    val orderIndex: Int,
    val level: JlptLevel,
    val category: String,
    val title: String,
    val description: String,
    val contentType: String,
    val source: String,
    val author: String?,
    val license: String,
    val createdAt: String,
    val version: Int,
    val paragraphs: List<Paragraph>,
    val questions: List<ComprehensionQuestion>
) {
    fun plainText(): String = paragraphs.joinToString(separator = "\n") { it.plainText() }
}

enum class MaterialStatus {
    LOCKED,
    AVAILABLE,
    IN_PROGRESS,
    COMPLETED
}

data class MaterialWithProgress(
    val material: ReadingMaterial,
    val status: MaterialStatus,
    val isFavorite: Boolean,
    val lastScrollProgress: Float,
    val completedAt: Long?
)

data class LevelProgress(
    val level: JlptLevel,
    val totalCount: Int,
    val completedCount: Int,
    val inProgressCount: Int
) {
    val percentage: Int
        get() = if (totalCount <= 0) 0 else ((completedCount * 100f) / totalCount).toInt().coerceIn(0, 100)

    val fraction: Float
        get() = if (totalCount <= 0) 0f else (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
}

data class DictionaryEntry(
    val id: String,
    val word: String,
    val reading: String,
    val translationRu: String,
    val partOfSpeech: String,
    val note: String? = null
)

data class SavedWord(
    val wordId: String,
    val word: String,
    val reading: String,
    val translationRu: String,
    val partOfSpeech: String,
    val note: String? = null,
    val savedAt: Long
)

data class StudyStats(
    val studyDays: Int,
    val currentStreak: Int,
    val totalCompleted: Int
)

data class WordReviewItem(
    val wordId: String,
    val word: String,
    val reading: String,
    val translationRu: String,
    val partOfSpeech: String,
    val repetitions: Int,
    val intervalDays: Int,
    val nextReviewAt: Long,
    val lastReviewedAt: Long?
)
