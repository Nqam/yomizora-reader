package com.yomizora.reader.data.repository

import com.yomizora.reader.data.local.content.ContentDatabase
import com.yomizora.reader.data.local.content.MaterialEntity
import com.yomizora.reader.domain.model.ComprehensionQuestion
import com.yomizora.reader.domain.model.ContentCategory
import com.yomizora.reader.domain.model.DictionaryEntry
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.Paragraph
import com.yomizora.reader.domain.model.ReadingMaterial
import com.yomizora.reader.domain.repository.ContentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentRepositoryImpl @Inject constructor(
    private val contentDatabase: ContentDatabase,
    private val json: Json
) : ContentRepository {

    private val materialDao = contentDatabase.materialDao()
    private val dictionaryDao = contentDatabase.dictionaryDao()
    private val materialCache = ConcurrentHashMap<String, ReadingMaterial>()
    private val dictionaryCache = ConcurrentHashMap<String, DictionaryEntry>()

    override fun observeMaterialsByLevel(level: JlptLevel): Flow<List<ReadingMaterial>> =
        materialDao.observeByLevel(level.code)
            .map { list -> list.map { it.toDomainCached() } }
            .flowOn(Dispatchers.Default)

    override fun observeAllMaterials(): Flow<List<ReadingMaterial>> =
        materialDao.observeAll()
            .map { list -> list.map { it.toDomainCached() } }
            .flowOn(Dispatchers.Default)

    override fun observeCategoriesByLevel(level: JlptLevel): Flow<List<String>> =
        materialDao.observeCategoriesByLevel(level.code)

    override suspend fun getMaterialById(id: String): ReadingMaterial? = withContext(Dispatchers.IO) {
        materialDao.getById(id)?.toDomainCached()
    }

    override suspend fun getNextMaterialInLevel(level: JlptLevel, currentOrderIndex: Int): ReadingMaterial? =
        withContext(Dispatchers.IO) {
            materialDao.getNextInLevel(level.code, currentOrderIndex)?.toDomainCached()
        }

    override suspend fun searchMaterials(
        query: String,
        level: JlptLevel?,
        category: String?
    ): List<ReadingMaterial> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        materialDao.search(trimmed, level?.code, category).map { it.toDomainCached() }
    }

    override suspend fun lookupWord(dictId: String?, surfaceText: String, reading: String?): DictionaryEntry? {
        if (!dictId.isNullOrBlank()) {
            dictionaryCache[dictId]?.let { return it }
        }
        val entity = withContext(Dispatchers.IO) {
            (if (!dictId.isNullOrBlank()) dictionaryDao.getById(dictId) else null)
                ?: (if (!reading.isNullOrBlank()) dictionaryDao.getByWordAndReading(surfaceText, reading) else null)
                ?: dictionaryDao.getByWord(surfaceText)
                ?: (if (!reading.isNullOrBlank()) dictionaryDao.getByReading(reading) else null)
                ?: dictionaryDao.getByReading(surfaceText)
        } ?: return null
        return DictionaryEntry(
            id = entity.id,
            word = entity.word,
            reading = entity.reading,
            translationRu = entity.translationRu,
            partOfSpeech = entity.partOfSpeech,
            note = entity.note
        ).also { dictionaryCache[it.id] = it }
    }

    override suspend fun lookupWordsByIds(ids: Collection<String>): Map<String, DictionaryEntry> {
        if (ids.isEmpty()) return emptyMap()
        val result = HashMap<String, DictionaryEntry>(ids.size)
        val missing = ArrayList<String>()
        for (id in ids) {
            val cached = dictionaryCache[id]
            if (cached != null) {
                result[id] = cached
            } else {
                missing += id
            }
        }
        if (missing.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                for (chunk in missing.chunked(500)) {
                    for (entity in dictionaryDao.getByIds(chunk)) {
                        val domain = DictionaryEntry(
                            id = entity.id,
                            word = entity.word,
                            reading = entity.reading,
                            translationRu = entity.translationRu,
                            partOfSpeech = entity.partOfSpeech,
                            note = entity.note
                        )
                        dictionaryCache[domain.id] = domain
                        result[domain.id] = domain
                    }
                }
            }
        }
        return result
    }

    override suspend fun getMaterialCountByLevel(level: JlptLevel): Int =
        materialDao.countByLevel(level.code)

    private fun MaterialEntity.toDomainCached(): ReadingMaterial {
        val cacheKey = "${id}_v${version}_o${orderIndex}"
        return materialCache.getOrPut(cacheKey) {
            ReadingMaterial(
                id = id,
                orderIndex = orderIndex,
                level = JlptLevel.fromCode(level),
                category = ContentCategory.toRussian(category),
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
    }
}
