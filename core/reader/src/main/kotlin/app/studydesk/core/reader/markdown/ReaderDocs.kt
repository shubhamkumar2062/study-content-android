package app.studydesk.core.reader.markdown

import app.studydesk.core.model.Probability
import app.studydesk.core.model.Question
import app.studydesk.core.model.SubjectPack

/** One readable document inside a subject: a unit, an extra (revision sheet) or a practical. */
data class ReaderDoc(
    val id: String,
    val title: String,
    val markdown: String,
    /** Unit number for units; null for extras and practicals. */
    val unit: Int?,
)

object ReaderDocs {
    fun unitId(number: Int) = "unit-$number"
    fun extraId(id: String) = "extra-$id"
    fun practicalId(id: String) = "practical-$id"

    /** All documents of a subject in reading order, grouped so prev/next stays inside a group. */
    fun all(pack: SubjectPack): List<List<ReaderDoc>> = listOf(
        pack.units.sortedBy { it.number }.map { ReaderDoc(unitId(it.number), it.title, it.markdown, it.number) },
        pack.extras.map { ReaderDoc(extraId(it.id), it.title, it.markdown, null) },
        pack.practicals.map { ReaderDoc(practicalId(it.id), it.title, it.markdown, null) },
    ).filter { it.isNotEmpty() }

    fun find(pack: SubjectPack, docId: String): Pair<ReaderDoc, List<ReaderDoc>>? {
        for (group in all(pack)) {
            group.firstOrNull { it.id == docId }?.let { return it to group }
        }
        return null
    }
}

/**
 * Puts each likely exam question under the section of the notes it is about, by matching
 * the important words of the heading and the question. Each question appears at most once,
 * under its best match; a weak match (fewer than two shared words) is not shown.
 */
object AskYourself {
    private val STOP = setOf(
        "the", "and", "of", "a", "an", "to", "in", "on", "for", "with", "its", "their", "is", "are", "what",
        "write", "explain", "give", "describe", "discuss", "short", "note", "notes", "brief", "briefly", "how",
        "why", "define", "state", "draw", "mention", "example", "examples", "uses", "use", "between", "from",
        "unit", "about", "by", "or", "be", "it", "this", "that", "into", "any", "two", "three",
    )

    fun place(blocks: List<Block>, questions: List<Question>, perSection: Int = 3): Map<String, List<Question>> {
        val headings = blocks.filterIsInstance<Block.Heading>().filter { it.level >= 2 }
        if (headings.isEmpty()) return emptyMap()
        val headingWords = headings.associate { it.anchor to words(it.plainText) }
        val candidates = questions.filter { it.rating != Probability.Low }.sortedBy { it.rating.rank }
        val placed = mutableMapOf<String, MutableList<Question>>()
        for (q in candidates) {
            val qWords = words(q.question)
            val best = headingWords
                .map { (anchor, hw) -> anchor to hw.count { it in qWords } }
                .filter { it.second >= 2 }
                .maxByOrNull { it.second } ?: continue
            val list = placed.getOrPut(best.first) { mutableListOf() }
            if (list.size < perSection) list += q
        }
        return placed
    }

    internal fun words(text: String): Set<String> =
        text.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length > 2 && it !in STOP && !it.all(Char::isDigit) }
            .map { it.removeSuffix("s") }.toSet()
}
