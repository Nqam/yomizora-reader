package com.yomizora.reader.domain.repository

import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<ReaderSettings>
    val activeProfileIdFlow: Flow<String>
    suspend fun setFontSize(fontSizeSp: Float)
    suspend fun setLineHeightMultiplier(multiplier: Float)
    suspend fun setShowFurigana(show: Boolean)
    suspend fun setProgressiveFurigana(enabled: Boolean)
    suspend fun setDisplayMode(mode: TextDisplayMode)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setActiveProfileId(profileId: String)
}
