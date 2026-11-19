package com.yomizora.reader.data.content

import androidx.room.withTransaction
import com.yomizora.reader.data.local.content.ContentDatabase
import com.yomizora.reader.data.local.content.ContentPackEntity
import com.yomizora.reader.data.local.content.DictionaryEntity
import com.yomizora.reader.data.local.content.MaterialEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentImporter @Inject constructor(
    private val contentDatabase: ContentDatabase,
    private val json: Json
) {
    companion object {
        const val SIMILARITY_THRESHOLD = 0.85
    }

    suspend fun importPackageJson(rawJson: String): ImportReport {
        val pkg = json.decodeFromString<ContentPackageDto>(rawJson)
        return importPackage(pkg)
    }

    suspend fun importPackage(pkg: ContentPackageDto): ImportReport {
        val errors = mutableListOf<String>()
        var inserted = 0
        var updated = 0
        var skipped = 0
        var rejected = 0

        contentDatabase.withTransaction {
            val materialDao = contentDatabase.materialDao()
            val dictionaryDao = contentDatabase.dictionaryDao()
            val packDao = contentDatabase.contentPackDao()

            if (pkg.dictionary.isNotEmpty()) {
                val dictEntities = pkg.dictionary.map { d ->
                    DictionaryEntity(
                        id = d.id,
                        word = d.word,
                        reading = d.reading,
                        translationRu = d.translationRu,
                        partOfSpeech = d.partOfSpeech,
                        note = d.note
                    )
                }
                dictionaryDao.upsertAll(dictEntities)
            }

            val existingMaterials = materialDao.getAllOnce().associateBy { it.id }.toMutableMap()
            val existingHashes = existingMaterials.values.associateBy { it.normalizedTextHash }.toMutableMap()
            val existingTitlesByLevel = existingMaterials.values
                .groupBy { it.level }
                .mapValues { (_, list) -> list.associateBy { it.title }.toMutableMap() }
                .toMutableMap()
            val maxOrderByLevel = mutableMapOf<String, Int>()

            for (dto in pkg.materials) {
                val validation = ContentValidator.validateMaterialStructure(dto)
                if (!validation.isValid) {
                    rejected++
                    errors += validation.errors
                    continue
                }

                val plainText = dto.paragraphs.joinToString("") { p ->
                    p.sentences.joinToString("") { s ->
                        s.segments.joinToString("") { it.text }
                    }
                }
                val normalizedText = ContentValidator.normalizeJapaneseText(plainText)
                val hash = ContentValidator.sha256(normalizedText)

                val existing = existingMaterials[dto.id]
                if (existing != null) {
                    if (dto.version > existing.version) {
                        val updatedEntity = dto.toEntity(
                            orderIndex = dto.orderIndex ?: existing.orderIndex,
                            normalizedText = normalizedText,
                            hash = hash
                        )
                        materialDao.upsert(updatedEntity)
                        existingMaterials[dto.id] = updatedEntity
                        updated++
                    } else {
                        skipped++
                    }
                    continue
                }

                // Uniqueness checks for new materials
                val hashMatch = existingHashes[hash]
                if (hashMatch != null) {
                    rejected++
                    errors += "Duplicate text content for ${dto.id} (matches ${hashMatch.id})"
                    continue
                }

                val titleMatch = existingTitlesByLevel[dto.level]?.get(dto.title)
                if (titleMatch != null) {
                    rejected++
                    errors += "Duplicate title '${dto.title}' in ${dto.level} for ${dto.id} (matches ${titleMatch.id})"
                    continue
                }

                val nearDuplicate = existingMaterials.values.firstOrNull { other ->
                    other.level == dto.level &&
                        ContentValidator.textSimilarity(normalizedText, other.normalizedText) >= SIMILARITY_THRESHOLD
                }
                if (nearDuplicate != null) {
                    rejected++
                    errors += "Near-duplicate text for ${dto.id} (similar to ${nearDuplicate.id})"
                    continue
                }

                val currentMaxOrder = maxOrderByLevel.getOrPut(dto.level) {
                    materialDao.maxOrderIndexByLevel(dto.level) ?: 0
                }
                val resolvedOrder = dto.orderIndex ?: (currentMaxOrder + 1)
                if (resolvedOrder > currentMaxOrder) {
                    maxOrderByLevel[dto.level] = resolvedOrder
                }

                val entity = dto.toEntity(
                    orderIndex = resolvedOrder,
                    normalizedText = normalizedText,
                    hash = hash
                )
                materialDao.upsert(entity)
                existingMaterials[dto.id] = entity
                existingHashes[hash] = entity
                existingTitlesByLevel.getOrPut(dto.level) { mutableMapOf() }[dto.title] = entity
                inserted++
            }

            packDao.upsert(
                ContentPackEntity(
                    packId = pkg.packId,
                    version = pkg.packVersion,
                    importedAt = System.currentTimeMillis(),
                    itemCount = inserted + updated
                )
            )
        }

        return ImportReport(
            packId = pkg.packId,
            insertedCount = inserted,
            updatedCount = updated,
            skippedCount = skipped,
            rejectedCount = rejected,
            errors = errors
        )
    }

    private fun MaterialDto.toEntity(
        orderIndex: Int,
        normalizedText: String,
        hash: String
    ): MaterialEntity = MaterialEntity(
        id = id,
        orderIndex = orderIndex,
        level = level.uppercase(),
        category = category,
        title = title,
        description = description,
        contentType = contentType,
        source = source,
        author = author,
        license = license,
        createdAt = createdAt,
        version = version,
        normalizedText = normalizedText,
        normalizedTextHash = hash,
        paragraphsJson = json.encodeToString(paragraphs),
        questionsJson = json.encodeToString(questions)
    )
}
