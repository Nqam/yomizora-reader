package com.yomizora.reader.data.content

import com.yomizora.reader.domain.model.JlptLevel
import java.security.MessageDigest

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

object ContentValidator {
    private val ID_REGEX = Regex("^[a-z0-9_]{3,64}$")
    private val KANJI_REGEX = Regex("[\\u4E00-\\u9FAF\\u3400-\\u4DBF]")
    private val WHITESPACE_AND_PUNCT_REGEX = Regex("[\\s\\p{Punct}。、「」！？…・（）『』【】]+")

    fun validateMaterialStructure(dto: MaterialDto): ValidationResult {
        val errors = mutableListOf<String>()

        if (!ID_REGEX.matches(dto.id)) {
            errors += "Invalid id '${dto.id}' (must match ^[a-z0-9_]{3,64}$)"
        }
        if (JlptLevel.entries.none { it.code.equals(dto.level, ignoreCase = true) }) {
            errors += "Invalid JLPT level '${dto.level}' for ${dto.id}"
        }
        if (dto.category.isBlank()) errors += "Blank category for ${dto.id}"
        if (dto.title.isBlank()) errors += "Blank title for ${dto.id}"
        if (dto.description.isBlank()) errors += "Blank description for ${dto.id}"
        if (dto.source.isBlank()) errors += "Blank source for ${dto.id}"
        if (dto.license.isBlank()) errors += "Blank license for ${dto.id}"
        if (dto.version < 1) errors += "Invalid version ${dto.version} for ${dto.id}"

        if (dto.paragraphs.isEmpty()) {
            errors += "Paragraphs cannot be empty for ${dto.id}"
        } else {
            var totalSegments = 0
            dto.paragraphs.forEachIndexed { pIdx, p ->
                if (p.sentences.isEmpty()) {
                    errors += "Paragraph $pIdx has no sentences in ${dto.id}"
                }
                p.sentences.forEachIndexed { sIdx, s ->
                    if (s.segments.isEmpty()) {
                        errors += "Sentence $pIdx.$sIdx has no segments in ${dto.id}"
                    }
                    s.segments.forEachIndexed { segIdx, seg ->
                        totalSegments++
                        if (seg.text.isEmpty()) {
                            errors += "Empty segment text at $pIdx.$sIdx.$segIdx in ${dto.id}"
                        } else if (KANJI_REGEX.containsMatchIn(seg.text) && seg.reading.isNullOrBlank()) {
                            errors += "Kanji segment '${seg.text}' missing furigana reading in ${dto.id}"
                        }
                    }
                }
            }
            if (totalSegments == 0) errors += "No text segments found in ${dto.id}"
        }

        if (dto.questions.size !in 1..5) {
            errors += "Questions count must be 1..5 (found ${dto.questions.size}) for ${dto.id}"
        } else {
            val qTexts = mutableSetOf<String>()
            dto.questions.forEachIndexed { idx, q ->
                if (q.question.isBlank()) errors += "Question $idx is blank in ${dto.id}"
                if (!qTexts.add(q.question.trim())) {
                    errors += "Duplicate question text '${q.question}' in ${dto.id}"
                }
                if (q.options.size !in 3..4) {
                    errors += "Question ${q.id} must have 3..4 options (found ${q.options.size}) in ${dto.id}"
                }
                if (q.options.any { it.isBlank() }) {
                    errors += "Question ${q.id} has blank option in ${dto.id}"
                }
                if (q.correctAnswer !in q.options.indices) {
                    errors += "Question ${q.id} correct_answer=${q.correctAnswer} out of bounds in ${dto.id}"
                }
                if (q.explanation.isBlank()) {
                    errors += "Question ${q.id} missing explanation in ${dto.id}"
                }
            }
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    fun normalizeJapaneseText(raw: String): String =
        raw.replace(WHITESPACE_AND_PUNCT_REGEX, "").trim()

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Character 3-gram Jaccard similarity in [0.0, 1.0] for near-duplicate detection.
     */
    fun textSimilarity(normalizedA: String, normalizedB: String): Double {
        if (normalizedA.isEmpty() && normalizedB.isEmpty()) return 1.0
        if (normalizedA.length < 3 || normalizedB.length < 3) {
            return if (normalizedA == normalizedB) 1.0 else 0.0
        }
        val gramsA = (0..normalizedA.length - 3).map { normalizedA.substring(it, it + 3) }.toSet()
        val gramsB = (0..normalizedB.length - 3).map { normalizedB.substring(it, it + 3) }.toSet()
        val intersection = gramsA.intersect(gramsB).size
        val union = gramsA.union(gramsB).size
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }
}
