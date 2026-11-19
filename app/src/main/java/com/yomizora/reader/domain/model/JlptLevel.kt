package com.yomizora.reader.domain.model

enum class JlptLevel(
    val code: String,
    val titleRu: String,
    val subtitleRu: String,
    val isAvailableInV1: Boolean
) {
    N5("N5", "JLPT N5", "Начальный уровень", true),
    N4("N4", "JLPT N4", "Базовый уровень", true),
    N3("N3", "JLPT N3", "Средний уровень", true),
    N2("N2", "JLPT N2", "Продвинутый уровень", true),
    N1("N1", "JLPT N1", "Высший уровень", true);

    companion object {
        fun fromCode(code: String): JlptLevel =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: N5

        val activeLevels: List<JlptLevel>
            get() = entries.filter { it.isAvailableInV1 }
    }
}
