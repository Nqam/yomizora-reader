package com.yomizora.reader.domain.model

object ContentCategory {
    private val russianNames = mapOf(
        "Stories" to "Рассказы",
        "Daily Life" to "Повседневная жизнь",
        "Dialogue" to "Диалоги",
        "Dialogues" to "Диалоги",
        "News" to "Новости",
        "Culture" to "Культура Японии",
        "Japanese Culture" to "Культура Японии",
        "Fairy Tales" to "Сказки",
        "Humor" to "Юмор",
        "Interesting Facts" to "Интересные факты",
        "Travel" to "Путешествия",
        "Food" to "Еда",
        "School" to "Образование",
        "Education" to "Образование",
        "Work" to "Работа",
        "Literature" to "Литература",
        "Adventure" to "Приключения"
    )

    fun toRussian(category: String): String = russianNames[category] ?: category
}
