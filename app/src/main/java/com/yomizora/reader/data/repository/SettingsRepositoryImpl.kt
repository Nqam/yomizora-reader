package com.yomizora.reader.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yomizora.reader.domain.model.ReaderSettings
import com.yomizora.reader.domain.model.TextDisplayMode
import com.yomizora.reader.domain.model.ThemeMode
import com.yomizora.reader.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yomizora_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_HEIGHT_MULT = floatPreferencesKey("line_height_multiplier")
        val SHOW_FURIGANA = booleanPreferencesKey("show_furigana")
        val PROGRESSIVE_FURIGANA = booleanPreferencesKey("progressive_furigana")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val TTS_VOICE_NAME = stringPreferencesKey("tts_voice_name")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    override val activeProfileIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE_ID] ?: "default"
    }

    override val settingsFlow: Flow<ReaderSettings> = context.dataStore.data.map { prefs ->
        ReaderSettings(
            fontSizeSp = (prefs[Keys.FONT_SIZE_SP] ?: 20f).coerceIn(14f, 32f),
            lineHeightMultiplier = (prefs[Keys.LINE_HEIGHT_MULT] ?: 1.65f).coerceIn(1.3f, 2.2f),
            showFurigana = prefs[Keys.SHOW_FURIGANA] ?: true,
            progressiveFurigana = prefs[Keys.PROGRESSIVE_FURIGANA] ?: true,
            displayMode = prefs[Keys.DISPLAY_MODE]?.let { name ->
                runCatching { TextDisplayMode.valueOf(name) }.getOrNull()
            } ?: TextDisplayMode.KANJI_WITH_FURIGANA,
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            ttsSpeechRate = (prefs[Keys.TTS_SPEECH_RATE] ?: 1.0f).coerceIn(0.5f, 1.6f),
            ttsPitch = (prefs[Keys.TTS_PITCH] ?: 1.0f).coerceIn(0.75f, 1.35f),
            ttsVoiceName = prefs[Keys.TTS_VOICE_NAME]?.takeIf { it.isNotBlank() }
        )
    }

    override suspend fun setFontSize(fontSizeSp: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE_SP] = fontSizeSp.coerceIn(14f, 32f) }
    }

    override suspend fun setLineHeightMultiplier(multiplier: Float) {
        context.dataStore.edit { it[Keys.LINE_HEIGHT_MULT] = multiplier.coerceIn(1.3f, 2.2f) }
    }

    override suspend fun setShowFurigana(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_FURIGANA] = show }
    }

    override suspend fun setProgressiveFurigana(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PROGRESSIVE_FURIGANA] = enabled }
    }

    override suspend fun setDisplayMode(mode: TextDisplayMode) {
        context.dataStore.edit { it[Keys.DISPLAY_MODE] = mode.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setTtsSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.TTS_SPEECH_RATE] = rate.coerceIn(0.5f, 1.6f) }
    }

    override suspend fun setTtsPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.TTS_PITCH] = pitch.coerceIn(0.75f, 1.35f) }
    }

    override suspend fun setTtsVoiceName(voiceName: String?) {
        context.dataStore.edit {
            if (voiceName.isNullOrBlank()) it.remove(Keys.TTS_VOICE_NAME)
            else it[Keys.TTS_VOICE_NAME] = voiceName
        }
    }

    override suspend fun setActiveProfileId(profileId: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId }
    }
}
