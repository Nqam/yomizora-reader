package com.yomizora.reader.data.local.content

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {
    @Query("SELECT * FROM materials WHERE level = :level ORDER BY orderIndex ASC")
    fun observeByLevel(level: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY level ASC, orderIndex ASC")
    fun observeAll(): Flow<List<MaterialEntity>>

    @Query("SELECT DISTINCT category FROM materials WHERE level = :level ORDER BY category ASC")
    fun observeCategoriesByLevel(level: String): Flow<List<String>>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MaterialEntity?

    @Query("SELECT * FROM materials WHERE level = :level ORDER BY orderIndex ASC")
    suspend fun getByLevelOnce(level: String): List<MaterialEntity>

    @Query("SELECT * FROM materials")
    suspend fun getAllOnce(): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE level = :level AND orderIndex > :currentOrderIndex ORDER BY orderIndex ASC LIMIT 1")
    suspend fun getNextInLevel(level: String, currentOrderIndex: Int): MaterialEntity?

    @Query("SELECT COUNT(*) FROM materials WHERE level = :level")
    suspend fun countByLevel(level: String): Int

    @Query("SELECT MAX(orderIndex) FROM materials WHERE level = :level")
    suspend fun maxOrderIndexByLevel(level: String): Int?

    @Query(
        """
        SELECT * FROM materials
        WHERE (:level IS NULL OR level = :level)
          AND (:category IS NULL OR category = :category)
          AND (
              title LIKE '%' || :query || '%'
              OR category LIKE '%' || :query || '%'
              OR level LIKE '%' || :query || '%'
              OR description LIKE '%' || :query || '%'
              OR normalizedText LIKE '%' || :query || '%'
          )
        ORDER BY level ASC, orderIndex ASC
        """
    )
    suspend fun search(query: String, level: String?, category: String?): List<MaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MaterialEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MaterialEntity)
}

@Dao
interface DictionaryDao {
    @Query("SELECT * FROM dictionary WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary WHERE word = :word AND reading = :reading LIMIT 1")
    suspend fun getByWordAndReading(word: String, reading: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary WHERE word = :word LIMIT 1")
    suspend fun getByWord(word: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary WHERE reading = :reading LIMIT 1")
    suspend fun getByReading(reading: String): DictionaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<DictionaryEntity>)
}

@Dao
interface ContentPackDao {
    @Query("SELECT * FROM content_packs WHERE packId = :packId LIMIT 1")
    suspend fun getById(packId: String): ContentPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pack: ContentPackEntity)
}
