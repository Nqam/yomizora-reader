package com.yomizora.reader

import com.yomizora.reader.data.content.ContentPackageDto
import com.yomizora.reader.data.content.ContentValidator
import com.yomizora.reader.data.content.MaterialDto
import com.yomizora.reader.domain.model.ComprehensionQuestion
import com.yomizora.reader.domain.model.Paragraph
import com.yomizora.reader.domain.model.Sentence
import com.yomizora.reader.domain.model.TextSegment
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ContentValidatorTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun sampleMaterial(
        id: String = "n5_test_001",
        reading: String? = "わたし",
        questionCount: Int = 1,
        correctAnswer: Int = 0
    ): MaterialDto {
        val questions = (1..questionCount).map { idx ->
            ComprehensionQuestion(
                id = "q$idx",
                question = "質問 $idx は何ですか。",
                options = listOf("A", "B", "C"),
                correctAnswer = correctAnswer,
                explanation = "解説です。"
            )
        }
        return MaterialDto(
            id = id,
            level = "N5",
            category = "Daily Life",
            title = "テストタイトル",
            description = "Описание теста",
            paragraphs = listOf(
                Paragraph(
                    sentences = listOf(
                        Sentence(
                            segments = listOf(
                                TextSegment(text = "私", reading = reading),
                                TextSegment(text = "は学生", reading = if (reading != null) "がくせい" else null),
                                TextSegment(text = "です。", reading = null)
                            )
                        )
                    )
                )
            ),
            questions = questions
        )
    }

    @Test
    fun `valid material passes validation`() {
        val result = ContentValidator.validateMaterialStructure(sampleMaterial())
        assertTrue(result.errors.joinToString(), result.isValid)
    }

    @Test
    fun `kanji without furigana fails validation`() {
        val result = ContentValidator.validateMaterialStructure(sampleMaterial(reading = null))
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("furigana") })
    }

    @Test
    fun `invalid correct answer index fails validation`() {
        val result = ContentValidator.validateMaterialStructure(sampleMaterial(correctAnswer = 5))
        assertFalse(result.isValid)
    }

    @Test
    fun `similarity detects near duplicates`() {
        val a = ContentValidator.normalizeJapaneseText("私は毎日学校へ行きます。日本語を勉強します。")
        val b = ContentValidator.normalizeJapaneseText("私は毎日学校へ行きます。日本語を勉強します！")
        val c = ContentValidator.normalizeJapaneseText("昨日、友達と京都へ旅行しました。とても楽しかったです。")

        assertEquals(1.0, ContentValidator.textSimilarity(a, b), 0.001)
        assertTrue(ContentValidator.textSimilarity(a, c) < 0.3)
    }

    @Test
    fun `starter asset packs contain at least 100 valid unique materials`() {
        val assetsDir = File("src/main/assets/content")
        assertTrue("Assets content directory should exist at ${assetsDir.absolutePath}", assetsDir.exists())

        val packFiles = assetsDir.listFiles { _, name -> name.endsWith(".json") }?.sorted() ?: emptyList()
        assertTrue(packFiles.isNotEmpty())

        val allMaterials = mutableListOf<MaterialDto>()
        val seenHashes = mutableSetOf<String>()
        val seenIds = mutableSetOf<String>()

        for (file in packFiles) {
            val pkg = json.decodeFromString<ContentPackageDto>(file.readText(Charsets.UTF_8))
            for (m in pkg.materials) {
                val res = ContentValidator.validateMaterialStructure(m)
                assertTrue("Material ${m.id} failed: ${res.errors}", res.isValid)
                assertTrue("Duplicate id ${m.id}", seenIds.add(m.id))

                val plain = m.paragraphs.joinToString("") { p ->
                    p.sentences.joinToString("") { s -> s.segments.joinToString("") { it.text } }
                }
                val hash = ContentValidator.sha256(ContentValidator.normalizeJapaneseText(plain))
                assertTrue("Duplicate content hash for ${m.id}", seenHashes.add(hash))
                allMaterials += m
            }
        }

        val n5Count = allMaterials.count { it.level.equals("N5", ignoreCase = true) }
        val n4Count = allMaterials.count { it.level.equals("N4", ignoreCase = true) }
        assertTrue("Expected >= 50 N5 texts, got $n5Count", n5Count >= 50)
        assertTrue("Expected >= 50 N4 texts, got $n4Count", n4Count >= 50)
        assertTrue("Expected >= 100 total texts, got ${allMaterials.size}", allMaterials.size >= 100)
    }
}
