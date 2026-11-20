package app.studydesk.feature.practice

import app.studydesk.core.database.CardStateEntity
import app.studydesk.core.model.Mcq
import app.studydesk.core.model.MockTest
import app.studydesk.core.model.Question
import app.studydesk.core.model.SubjectPack
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Flashcard scheduling with five boxes. "I know it" moves a card up a box and further out;
 * "Revise again" puts it back in box 1, due straight away (it comes round again today).
 */
object Leitner {
    private const val DAY = 24 * 60 * 60 * 1000L
    private val intervals = longArrayOf(0, DAY, 3 * DAY, 7 * DAY, 14 * DAY, 30 * DAY)

    fun next(packId: String, questionId: String, current: CardStateEntity?, knew: Boolean, now: Long): CardStateEntity {
        val box = if (knew) ((current?.box ?: 0) + 1).coerceAtMost(5) else 1
        val due = if (knew) now + intervals[box] else now
        return CardStateEntity(packId, questionId, box, due, now)
    }
}

/** One question shown in a mock paper, with what the student did. */
@Serializable
data class PaperItem(
    val id: String,
    val question: String,
    val answer: String,
    val options: List<String> = emptyList(),
    val correct: Int = -1,
    val explanation: String = "",
    val chosen: Int? = null,
    val selfMarks: Int? = null,
    val flagged: Boolean = false,
)

@Serializable
data class PaperSection(
    val id: String,
    val title: String,
    val source: String,
    val marksEach: Int,
    val attempt: Int,
    val items: List<PaperItem>,
) {
    val isMcq: Boolean get() = source == "mcq"
    val maxMarks: Int get() = marksEach * attempt

    /** MCQs are marked by the app; written answers by the student's own marks, best [attempt] counted. */
    val marks: Int
        get() = if (isMcq) {
            items.count { it.chosen != null && it.chosen == it.correct }.coerceAtMost(attempt) * marksEach
        } else {
            items.mapNotNull { it.selfMarks?.coerceIn(0, marksEach) }.sortedDescending().take(attempt).sum()
        }
}

@Serializable
data class Paper(val testId: String, val title: String, val durationMinutes: Int, val totalMarks: Int, val sections: List<PaperSection>) {
    val autoMarks: Int get() = sections.filter { it.isMcq }.sumOf { it.marks }
    val autoMax: Int get() = sections.filter { it.isMcq }.sumOf { it.maxMarks }
    val selfMarks: Int get() = sections.filterNot { it.isMcq }.sumOf { it.marks }
    val selfMax: Int get() = sections.filterNot { it.isMcq }.sumOf { it.maxMarks }
}

object PaperBuilder {
    /** Draws each section's questions from the subject's banks; the same [seed] gives the same paper. */
    fun build(pack: SubjectPack, test: MockTest, seed: Long): Paper {
        val random = Random(seed)
        val sections = test.sections.map { s ->
            val items = when (s.source) {
                "mcq" -> pack.mcqs.shuffled(random).take(s.count).map(::fromMcq)
                else -> pack.questions.filter { it.type == s.source }
                    // Prefer high-probability questions, like a real paper, but keep some variety.
                    // Each question gets its sort key once: a key that changes between comparisons
                    // breaks the sort ("Comparison method violates its general contract").
                    .map { it to it.rating.rank + random.nextDouble() * 1.5 }
                    .sortedBy { it.second }
                    .take(s.count).map { fromQuestion(it.first) }
            }
            PaperSection(s.id, s.title, s.source, s.marksEach, s.attempt, items)
        }
        return Paper(test.id, test.title, test.durationMinutes, test.totalMarks, sections)
    }

    private fun fromMcq(m: Mcq) = PaperItem(m.id, m.question, m.options[m.correct], m.options, m.correct, m.explanation)
    private fun fromQuestion(q: Question) = PaperItem(q.id, q.question, q.answer)
}

/** Which MCQs a practice run uses. */
object McqPicker {
    fun pick(all: List<Mcq>, unit: Int, wrongIds: Set<String>?, quick: Boolean, seed: Long): List<Mcq> {
        val random = Random(seed)
        var list = if (unit > 0) all.filter { it.unit == unit } else all
        if (wrongIds != null) list = list.filter { it.id in wrongIds }
        list = list.shuffled(random)
        return if (quick) list.take(10) else list
    }
}
