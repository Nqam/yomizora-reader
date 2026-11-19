package com.yomizora.reader

import com.yomizora.reader.domain.model.ComprehensionQuestion
import com.yomizora.reader.domain.model.JlptLevel
import com.yomizora.reader.domain.model.MaterialStatus
import com.yomizora.reader.domain.model.Paragraph
import com.yomizora.reader.domain.model.ReadingMaterial
import com.yomizora.reader.domain.model.Sentence
import com.yomizora.reader.domain.model.TextSegment
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressLockingLogicTest {

    private fun makeMaterial(id: String, level: JlptLevel, orderIndex: Int): ReadingMaterial =
        ReadingMaterial(
            id = id,
            orderIndex = orderIndex,
            level = level,
            category = "Stories",
            title = "Title $id",
            description = "Desc $id",
            contentType = "original",
            source = "Original",
            author = "Yomizora",
            license = "Original",
            createdAt = "2026-11-18",
            version = 1,
            paragraphs = listOf(
                Paragraph(listOf(Sentence(listOf(TextSegment("テスト", null)))))
            ),
            questions = listOf(
                ComprehensionQuestion(
                    id = "q1",
                    question = "Q?",
                    options = listOf("A", "B", "C"),
                    correctAnswer = 0,
                    explanation = "Exp"
                )
            )
        )

    private fun computeStatuses(
        materials: List<ReadingMaterial>,
        stateById: Map<String, String>
    ): Map<String, MaterialStatus> {
        val result = mutableMapOf<String, MaterialStatus>()
        var previousCompleted = true
        for (m in materials.sortedBy { it.orderIndex }) {
            val status = if (!previousCompleted) {
                MaterialStatus.LOCKED
            } else {
                when (stateById[m.id]) {
                    "COMPLETED" -> MaterialStatus.COMPLETED
                    "IN_PROGRESS" -> MaterialStatus.IN_PROGRESS
                    else -> MaterialStatus.AVAILABLE
                }
            }
            previousCompleted = (status == MaterialStatus.COMPLETED)
            result[m.id] = status
        }
        return result
    }

    @Test
    fun `first material is available and subsequent materials are locked`() {
        val n5 = listOf(
            makeMaterial("n5_1", JlptLevel.N5, 1),
            makeMaterial("n5_2", JlptLevel.N5, 2),
            makeMaterial("n5_3", JlptLevel.N5, 3)
        )
        val statuses = computeStatuses(n5, emptyMap())
        assertEquals(MaterialStatus.AVAILABLE, statuses["n5_1"])
        assertEquals(MaterialStatus.LOCKED, statuses["n5_2"])
        assertEquals(MaterialStatus.LOCKED, statuses["n5_3"])
    }

    @Test
    fun `completing first unlocks second while third stays locked`() {
        val n5 = listOf(
            makeMaterial("n5_1", JlptLevel.N5, 1),
            makeMaterial("n5_2", JlptLevel.N5, 2),
            makeMaterial("n5_3", JlptLevel.N5, 3)
        )
        val statuses = computeStatuses(n5, mapOf("n5_1" to "COMPLETED"))
        assertEquals(MaterialStatus.COMPLETED, statuses["n5_1"])
        assertEquals(MaterialStatus.AVAILABLE, statuses["n5_2"])
        assertEquals(MaterialStatus.LOCKED, statuses["n5_3"])
    }

    @Test
    fun `resetting one level does not affect another level`() {
        val n5 = listOf(
            makeMaterial("n5_1", JlptLevel.N5, 1),
            makeMaterial("n5_2", JlptLevel.N5, 2)
        )
        val n4 = listOf(
            makeMaterial("n4_1", JlptLevel.N4, 1),
            makeMaterial("n4_2", JlptLevel.N4, 2)
        )
        // Simulate N5 reset while N4 retains completion
        val n5Statuses = computeStatuses(n5, emptyMap())
        val n4Statuses = computeStatuses(n4, mapOf("n4_1" to "COMPLETED"))

        assertEquals(MaterialStatus.AVAILABLE, n5Statuses["n5_1"])
        assertEquals(MaterialStatus.LOCKED, n5Statuses["n5_2"])
        assertEquals(MaterialStatus.COMPLETED, n4Statuses["n4_1"])
        assertEquals(MaterialStatus.AVAILABLE, n4Statuses["n4_2"])
    }
}
