package app.studydesk.feature.practice

import app.studydesk.core.database.CardStateEntity
import app.studydesk.core.model.Mcq
import app.studydesk.core.model.MockTest
import app.studydesk.core.model.Question
import app.studydesk.core.model.SubjectPack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeLogicTest {
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun leitnerMovesUpOnKnowAndBackToBoxOneOnAgain() {
        val first = Leitner.next("p", "q", null, knew = true, now = 0)
        assertEquals(1, first.box)
        assertEquals(day, first.dueAt)
        val second = Leitner.next("p", "q", first, knew = true, now = day)
        assertEquals(2, second.box)
        assertEquals(day + 3 * day, second.dueAt)
        val again = Leitner.next("p", "q", second, knew = false, now = 5 * day)
        assertEquals(1, again.box)
        assertEquals(5 * day, again.dueAt)
        var s: CardStateEntity? = null
        repeat(8) { s = Leitner.next("p", "q", s, true, 0) }
        assertEquals(5, s!!.box)
    }

    private fun pack(): SubjectPack {
        val mcqs = (1..30).map { Mcq("m$it", 1, "Question $it?", listOf("a", "b", "c", "d"), it % 4, "because", "high") }
        val short = (1..12).map { Question("s$it", 1, "short", 5, if (it < 4) "high" else "low", "Short $it", "answer") }
        val long = (1..5).map { Question("l$it", 1, "long", 10, "medium", "Long $it", "answer") }
        return SubjectPack(1, "x", "X1", "X", "s", "p", "sc", "t", true, questions = short + long, mcqs = mcqs)
    }

    private val test = MockTest(
        "full", "Full", null, 180, 75,
        listOf(
            MockTest.Section("a", "A", "mcq", 1, 20, 20),
            MockTest.Section("b", "B", "long", 10, 3, 2),
            MockTest.Section("c", "C", "short", 5, 9, 7),
        ),
    )

    @Test
    fun paperHasTheRightCountsAndIsRepeatableForTheSameSeed() {
        val a = PaperBuilder.build(pack(), test, seed = 42)
        val b = PaperBuilder.build(pack(), test, seed = 42)
        assertEquals(listOf(20, 3, 9), a.sections.map { it.items.size })
        assertEquals(a, b)
        assertTrue(a.sections.all { s -> s.items.map { it.id }.toSet().size == s.items.size })
        assertEquals(75, a.sections.sumOf { it.maxMarks })
    }

    @Test
    fun markingCountsMcqsAutomaticallyAndOnlyTheBestWrittenAnswers() {
        val paper = PaperBuilder.build(pack(), test, seed = 1)
        val marked = paper.copy(
            sections = paper.sections.map { s ->
                when (s.source) {
                    // 15 right, 3 wrong, 2 skipped
                    "mcq" -> s.copy(items = s.items.mapIndexed { i, it -> it.copy(chosen = if (i < 15) it.correct else if (i < 18) (it.correct + 1) % 4 else null) })
                    // three essays written; only the best two count: 9 + 7
                    "long" -> s.copy(items = s.items.mapIndexed { i, it -> it.copy(selfMarks = listOf(7, 9, 4)[i]) })
                    // marks above the maximum are capped at 5; best seven of nine
                    else -> s.copy(items = s.items.mapIndexed { i, it -> it.copy(selfMarks = listOf(5, 4, 3, 5, 2, 1, 0, 8, 4)[i]) })
                }
            },
        )
        assertEquals(15, marked.autoMarks)
        assertEquals(20, marked.autoMax)
        assertEquals(16 + (5 + 5 + 5 + 4 + 4 + 3 + 2), marked.selfMarks)
        assertEquals(55, marked.selfMax)
    }

    @Test
    fun quickQuizTakesTenAndRetryKeepsOnlyWrongOnes() {
        val mcqs = pack().mcqs
        assertEquals(10, McqPicker.pick(mcqs, 0, null, quick = true, seed = 3).size)
        assertEquals(setOf("m1", "m2"), McqPicker.pick(mcqs, 0, setOf("m1", "m2"), quick = false, seed = 3).map { it.id }.toSet())
    }

    @Test
    fun largeBanksBuildAPaperForEverySeed() {
        // 150 written questions of each type, like a big D.Pharm subject. The sort used to crash here.
        val levels = listOf("high", "medium", "low")
        val written = (1..150).flatMap { n ->
            listOf("long", "short").map { type -> Question("$type-$n", 1, type, 5, levels[n % 3], "Q $n", "answer") }
        }
        val big = pack().copy(questions = written)
        repeat(200) { seed ->
            val paper = PaperBuilder.build(big, test, seed.toLong())
            assertEquals(listOf(20, 3, 9), paper.sections.map { it.items.size })
            assertTrue(paper.sections.all { s -> s.items.map { it.id }.toSet().size == s.items.size })
        }
    }

    @Test
    fun aPaperWithoutMcqsIsBuiltAndMarkedFromWrittenAnswersOnly() {
        // The PCI 35-mark paper: long 1 of 2 x 10, short 5 of 7 x 5, no MCQ section.
        val short35 = MockTest(
            "full", "Full", null, 90, 35,
            listOf(MockTest.Section("a", "I", "long", 10, 2, 1), MockTest.Section("b", "II", "short", 5, 7, 5)),
        )
        val paper = PaperBuilder.build(pack(), short35, seed = 3)
        assertEquals(listOf(2, 7), paper.sections.map { it.items.size })
        val marked = paper.copy(
            sections = paper.sections.map { s ->
                s.copy(items = s.items.mapIndexed { i, it -> it.copy(selfMarks = if (s.source == "long") listOf(6, 8)[i] else 4) })
            },
        )
        assertEquals(0, marked.autoMax)
        assertEquals(8 + 5 * 4, marked.selfMarks)
        assertEquals(35, marked.selfMax)
    }
}
