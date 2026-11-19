package com.yomizora.reader.data.local.content

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "materials",
    indices = [
        Index(value = ["level", "orderIndex"]),
        Index(value = ["normalizedTextHash"]),
        Index(value = ["title"])
    ]
)
data class MaterialEntity(
    @PrimaryKey val id: String,
    val orderIndex: Int,
    val level: String,
    val category: String,
    val title: String,
    val description: String,
    val contentType: String,
    val source: String,
    val author: String?,
    val license: String,
    val createdAt: String,
    val version: Int,
    val normalizedText: String,
    val normalizedTextHash: String,
    val paragraphsJson: String,
    val questionsJson: String
)

@Entity(
    tableName = "dictionary",
    indices = [
        Index(value = ["word"]),
        Index(value = ["reading"])
    ]
)
data class DictionaryEntity(
    @PrimaryKey val id: String,
    val word: String,
    val reading: String,
    val translationRu: String,
    val partOfSpeech: String,
    val note: String?
)

@Entity(tableName = "content_packs")
data class ContentPackEntity(
    @PrimaryKey val packId: String,
    val version: Int,
    val importedAt: Long,
    val itemCount: Int
)
