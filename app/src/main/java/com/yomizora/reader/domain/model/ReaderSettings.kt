package com.yomizora.reader.domain.model

enum class TextDisplayMode {
    KANJI_WITH_FURIGANA,
    KANJI_ONLY,
    KANA_ONLY
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ReaderSettings(
    val fontSizeSp: Float = 20f,
    val lineHeightMultiplier: Float = 1.65f,
    val showFurigana: Boolean = true,
    val progressiveFurigana: Boolean = true,
    val displayMode: TextDisplayMode = TextDisplayMode.KANJI_WITH_FURIGANA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVoiceName: String? = null
)
