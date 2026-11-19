package com.yomizora.reader.data.repository

import com.yomizora.reader.data.local.content.ContentDatabase
import com.yomizora.reader.data.local.content.MaterialEntity
import com.yomizora.reader.domain.model.ComprehensionQuestion
import com.yomizora.reader.domain.model.DictionaryEntry
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.Paragraph
import com.yomizora.reader.domain.model.ReadingMaterial
import com.yomizora.reader.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val contentDatabase: ContentDatabase,
    private val json: Json
) : ContentRepository {

    private val materialDao = contentDatabase.materialDao()
    private val dictionaryDao = contentDatabase.dictionaryDao()

    override fun observeMaterialsByLevel(level: JlptLevel): Flow<List<ReadingMaterial>> =
        materialDao.observeByLevel(level.code).map { list -> list.map { it.toDomain() } }

    override fun observeAllMaterials(): Flow<List<ReadingMaterial>> =
        materialDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCategoriesByLevel(level: JlptLevel): Flow<List<String>> =
        materialDao.observeCategoriesByLevel(level.code)

    override suspend fun getMaterialById(id: String): ReadingMaterial? =
        materialDao.getById(id)?.toDomain()

    override suspend fun getNextMaterialInLevel(level: JlptLevel, currentOrderIndex: Int): ReadingMaterial? =
        materialDao.getNextInLevel(level.code, currentOrderIndex)?.toDomain()

    override suspend fun searchMaterials(
        query: String,
        level: JlptLevel?,
        category: String?
    ): List<ReadingMaterial> {
        val trimmed = query.trim()
        return materialDao.search(trimmed, level?.code, category).map { it.toDomain() }
    }

    override suspend fun lookupWord(dictId: String?, surfaceText: String, reading: String?): DictionaryEntry? {
        val entity = (if (!dictId.isNullOrBlank()) dictionaryDao.getById(dictId) else null)
            ?: (if (!reading.isNullOrBlank()) dictionaryDao.getByWordAndReading(surfaceText, reading) else null)
            ?: dictionaryDao.getByWord(surfaceText)
            ?: (if (!reading.isNullOrBlank()) dictionaryDao.getByReading(reading) else null)
            ?: dictionaryDao.getByReading(surfaceText)
            ?: return null
        return DictionaryEntry(
            id = entity.id,
            word = entity.word,
            reading = entity.reading,
            translationRu = entity.translationRu,
            partOfSpeech = entity.partOfSpeech,
            note = entity.note
        )
    }

    override suspend fun getMaterialCountByLevel(level: JlptLevel): Int =
        materialDao.countByLevel(level.code)

    private fun MaterialEntity.toDomain(): ReadingMaterial = ReadingMaterial(
        id = id,
        orderIndex = orderIndex,
        level = JlptLevel.fromCode(level),
        category = category,
        title = title,
        description = description,
        contentType = contentType,
        source = source,
        author = author,
        license = license,
        createdAt = createdAt,
        version = version,
        paragraphs = json.decodeFromString<List<Paragraph>>(paragraphsJson),
        questions = json.decodeFromString<List<ComprehensionQuestion>>(questionsJson)
    )
}
