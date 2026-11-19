package com.yomizora.reader.data.local.content

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MaterialEntity::class,
        DictionaryEntity::class,
        ContentPackEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun materialDao(): MaterialDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun contentPackDao(): ContentPackDao
}
