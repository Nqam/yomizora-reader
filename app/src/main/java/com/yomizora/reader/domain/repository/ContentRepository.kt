package com.yomizora.reader.domain.repository

import com.yomizora.reader.domain.model.DictionaryEntry
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.ReadingMaterial
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun observeMaterialsByLevel(level: JlptLevel): Flow<List<ReadingMaterial>>
    fun observeAllMaterials(): Flow<List<ReadingMaterial>>
    fun observeCategoriesByLevel(level: JlptLevel): Flow<List<String>>
    suspend fun getMaterialById(id: String): ReadingMaterial?
    suspend fun getNextMaterialInLevel(level: JlptLevel, currentOrderIndex: Int): ReadingMaterial?
    suspend fun searchMaterials(query: String, level: JlptLevel? = null, category: String? = null): List<ReadingMaterial>
    suspend fun lookupWord(dictId: String?, surfaceText: String, reading: String? = null): DictionaryEntry?
    suspend fun getMaterialCountByLevel(level: JlptLevel): Int
}
