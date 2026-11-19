package com.yomizora.reader.data.local.progress

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        MaterialProgressEntity::class,
        SavedWordEntity::class,
        StudyActivityEntity::class,
        UserStateEntity::class,
        WordReviewEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class UserProgressDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun materialProgressDao(): MaterialProgressDao
    abstract fun savedWordDao(): SavedWordDao
    abstract fun studyActivityDao(): StudyActivityDao
    abstract fun userStateDao(): UserStateDao
    abstract fun wordReviewDao(): WordReviewDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `profiles` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `profiles` (`id`, `name`, `createdAt`) VALUES ('default', 'Основной профиль', ${System.currentTimeMillis()})"
                )

                // Migrate material_progress
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `material_progress_new` (
                        `profileId` TEXT NOT NULL,
                        `materialId` TEXT NOT NULL,
                        `level` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `lastOpenedAt` INTEGER,
                        `completedAt` INTEGER,
                        `lastScrollProgress` REAL NOT NULL,
                        `attemptsCount` INTEGER NOT NULL,
                        `lastAnswersJson` TEXT,
                        PRIMARY KEY(`profileId`, `materialId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `material_progress_new`
                    SELECT 'default', materialId, level, state, isFavorite, lastOpenedAt, completedAt, lastScrollProgress, attemptsCount, lastAnswersJson
                    FROM `material_progress`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `material_progress`")
                db.execSQL("ALTER TABLE `material_progress_new` RENAME TO `material_progress`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_material_progress_profileId_level` ON `material_progress` (`profileId`, `level`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_material_progress_profileId_isFavorite` ON `material_progress` (`profileId`, `isFavorite`)")

                // Migrate saved_words
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_words_new` (
                        `profileId` TEXT NOT NULL,
                        `wordId` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `reading` TEXT NOT NULL,
                        `translationRu` TEXT NOT NULL,
                        `partOfSpeech` TEXT NOT NULL,
                        `note` TEXT,
                        `savedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`profileId`, `wordId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `saved_words_new`
                    SELECT 'default', wordId, word, reading, translationRu, partOfSpeech, note, savedAt
                    FROM `saved_words`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `saved_words`")
                db.execSQL("ALTER TABLE `saved_words_new` RENAME TO `saved_words`")

                // Migrate study_activity
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_activity_new` (
                        `profileId` TEXT NOT NULL,
                        `dateIso` TEXT NOT NULL,
                        `textsOpened` INTEGER NOT NULL,
                        `textsCompleted` INTEGER NOT NULL,
                        PRIMARY KEY(`profileId`, `dateIso`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `study_activity_new`
                    SELECT 'default', dateIso, textsOpened, textsCompleted
                    FROM `study_activity`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `study_activity`")
                db.execSQL("ALTER TABLE `study_activity_new` RENAME TO `study_activity`")

                // Migrate user_state
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_state_new` (
                        `profileId` TEXT NOT NULL,
                        `lastOpenedMaterialId` TEXT,
                        `lastOpenedLevel` TEXT,
                        PRIMARY KEY(`profileId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `user_state_new`
                    SELECT 'default', lastOpenedMaterialId, lastOpenedLevel
                    FROM `user_state`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `user_state`")
                db.execSQL("ALTER TABLE `user_state_new` RENAME TO `user_state`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_reviews` (
                        `profileId` TEXT NOT NULL,
                        `wordId` TEXT NOT NULL,
                        `word` TEXT NOT NULL,
                        `reading` TEXT NOT NULL,
                        `translationRu` TEXT NOT NULL,
                        `partOfSpeech` TEXT NOT NULL,
                        `repetitions` INTEGER NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `nextReviewAt` INTEGER NOT NULL,
                        `lastReviewedAt` INTEGER,
                        PRIMARY KEY(`profileId`, `wordId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_word_reviews_profileId_nextReviewAt` ON `word_reviews` (`profileId`, `nextReviewAt`)"
                )
            }
        }
    }
}
