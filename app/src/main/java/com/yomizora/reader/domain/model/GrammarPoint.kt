package com.yomizora.reader.domain.model

data class GrammarPoint(
    val id: String,
    val pattern: String,
    val level: JlptLevel,
    val titleRu: String,
    val formula: String,
    val explanationRu: String,
    val exampleJp: String,
    val exampleRu: String
)
