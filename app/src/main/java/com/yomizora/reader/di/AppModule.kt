package com.yomizora.reader.di

import android.content.Context
import androidx.room.Room
import com.yomizora.reader.data.local.content.ContentDatabase
import com.yomizora.reader.data.local.progress.UserProgressDatabase
import com.yomizora.reader.data.repository.ContentRepositoryImpl
import com.yomizora.reader.data.repository.ProgressRepositoryImpl
import com.yomizora.reader.data.repository.SettingsRepositoryImpl
import com.yomizora.reader.domain.repository.ContentRepository
import com.yomizora.reader.domain.repository.ProgressRepository
import com.yomizora.reader.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    @Provides
    @Singleton
    fun provideContentDatabase(
        @ApplicationContext context: Context
    ): ContentDatabase = Room.databaseBuilder(
        context,
        ContentDatabase::class.java,
        "yomizora_content.db"
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    @Singleton
    fun provideUserProgressDatabase(
        @ApplicationContext context: Context
    ): UserProgressDatabase = Room.databaseBuilder(
        context,
        UserProgressDatabase::class.java,
        "yomizora_progress.db"
    )
        .addMigrations(
            UserProgressDatabase.MIGRATION_1_2,
            UserProgressDatabase.MIGRATION_2_3
        )
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
