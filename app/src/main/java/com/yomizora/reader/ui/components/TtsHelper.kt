package com.yomizora.reader.ui.components

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import com.yomizora.reader.domain.model.ReaderSettings
import java.util.Locale

data class JapaneseVoiceOption(
    val name: String,
    val label: String,
    val isOffline: Boolean
)

fun TextToSpeech.listJapaneseVoices(): List<JapaneseVoiceOption> {
    val jaVoices = runCatching {
        voices.orEmpty()
            .filter { it.locale?.language.equals("ja", ignoreCase = true) }
            .sortedWith(compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }.thenBy { it.name })
    }.getOrDefault(emptyList())

    return jaVoices.mapIndexed { index, voice ->
        val isOffline = !voice.isNetworkConnectionRequired
        val modeLabel = if (isOffline) "офлайн" else "онлайн"
        val shortName = voice.name
            .removePrefix("ja-jp-x-")
            .removePrefix("ja-JP-language")
            .takeIf { it.isNotBlank() && it.length <= 14 }
        val display = if (shortName != null) {
            "Голос ${index + 1} · $shortName ($modeLabel)"
        } else {
            "Голос ${index + 1} ($modeLabel)"
        }
        JapaneseVoiceOption(
            name = voice.name,
            label = display,
            isOffline = isOffline
        )
    }
}

fun TextToSpeech.applyJapaneseTtsSettings(settings: ReaderSettings): Boolean {
    val res = setLanguage(Locale.JAPANESE)
    val available = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
    if (available) {
        val jaVoices = runCatching {
            voices.orEmpty().filter { it.locale?.language.equals("ja", ignoreCase = true) }
        }.getOrDefault(emptyList())
        val chosen = if (!settings.ttsVoiceName.isNullOrBlank()) {
            jaVoices.firstOrNull { it.name == settings.ttsVoiceName }
                ?: jaVoices.firstOrNull { !it.isNetworkConnectionRequired }
        } else {
            jaVoices.firstOrNull { !it.isNetworkConnectionRequired }
        }
        if (chosen != null) {
            runCatching { voice = chosen }
        }
    }
    setSpeechRate(settings.ttsSpeechRate)
    setPitch(settings.ttsPitch)
    return available
}

fun openInstallTtsData(context: Context) {
    val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(installIntent)
    }.onFailure {
        runCatching { context.startActivity(fallbackIntent) }
    }
}
