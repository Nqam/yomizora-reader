package com.yomizora.reader.data.content

import com.yomizora.reader.domain.model.ComprehensionQuestion
import com.yomizora.reader.domain.model.Paragraph
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContentPackageDto(
    @SerialName("pack_id")
    val packId: String,
    @SerialName("pack_version")
    val packVersion: Int = 1,
    val materials: List<MaterialDto> = emptyList(),
    val dictionary: List<DictionaryDto> = emptyList()
)

@Serializable
data class MaterialDto(
    val id: String,
    @SerialName("order_index")
    val orderIndex: Int? = null,
    val level: String,
    val category: String,
    val title: String,
    val description: String,
    @SerialName("content_type")
    val contentType: String = "original",
    val source: String = "Original content",
    val author: String? = null,
    val license: String = "Original",
    @SerialName("created_at")
    val createdAt: String = "2026-11-18",
    val version: Int = 1,
    val paragraphs: List<Paragraph>,
    val questions: List<ComprehensionQuestion>
)

@Serializable
data class DictionaryDto(
    val id: String,
    val word: String,
    val reading: String,
    @SerialName("translation_ru")
    val translationRu: String,
    @SerialName("part_of_speech")
    val partOfSpeech: String,
    val note: String? = null
)

data class ImportReport(
    val packId: String,
    val insertedCount: Int,
    val updatedCount: Int,
    val skippedCount: Int,
    val rejectedCount: Int,
    val errors: List<String>
)
