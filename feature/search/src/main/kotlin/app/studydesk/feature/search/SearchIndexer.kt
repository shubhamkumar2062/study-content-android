package app.studydesk.feature.search

import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.ContentVisibility
import app.studydesk.core.database.SearchDao
import app.studydesk.core.database.SearchEntryEntity
import app.studydesk.core.model.SubjectPack
import app.studydesk.core.reader.markdown.Block
import app.studydesk.core.reader.markdown.MarkdownParser
import app.studydesk.core.reader.markdown.ReaderDocs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the search index in step with the subjects on the phone, re-indexing only what changed.
 * Only subjects this build may open are indexed, so release builds never surface unreviewed notes.
 */
@Singleton
class SearchIndexer @Inject constructor(
    private val content: ContentRepository,
    private val dao: SearchDao,
    private val visibility: ContentVisibility,
) {
    private val mutex = Mutex()

    suspend fun ensureIndexed() = mutex.withLock {
        withContext(Dispatchers.Default) {
            val active = content.localState.value?.active?.packs.orEmpty()
                .filter { visibility.canOpen(it.reviewed) }
                .associate { it.id to it.sha256 }
            val indexed = dao.indexed().associate { it.packId to it.sha256 }
            (indexed.keys - active.keys).forEach {
                dao.deleteEntries(it)
                dao.unmark(it)
            }
            val stale = active.filter { (id, sha) -> indexed[id] != sha }.keys
            if (stale.isEmpty()) return@withContext
            content.loadAllPacks().filter { it.subjectId in stale }.forEach { pack ->
                dao.replace(pack.subjectId, active.getValue(pack.subjectId), entries(pack))
            }
        }
    }

    companion object {
        /** One entry per section of notes (heading + its text), per question, per MCQ. */
        fun entries(pack: SubjectPack): List<SearchEntryEntity> = buildList {
            ReaderDocs.all(pack).flatten().forEach { doc ->
                val kind = if (doc.id.startsWith("practical-")) "practical" else "note"
                val blocks = MarkdownParser.parse(doc.markdown)
                var title = doc.title
                var start = 0
                val body = StringBuilder()
                fun flush() {
                    if (body.isNotBlank()) add(SearchEntryEntity(pack.subjectId, pack.title, kind, "${doc.id}#$start", title, body.toString().trim()))
                    body.clear()
                }
                blocks.forEachIndexed { i, b ->
                    if (b is Block.Heading && b.level >= 2) {
                        flush()
                        title = "${doc.title} › ${b.plainText}"
                        start = i
                    } else if (b !is Block.Image && b != Block.Rule) {
                        body.append(b.plainText).append('\n')
                    }
                }
                flush()
            }
            pack.questions.forEach { add(SearchEntryEntity(pack.subjectId, pack.title, "question", it.id, it.question, it.answer)) }
            pack.mcqs.forEach { add(SearchEntryEntity(pack.subjectId, pack.title, "mcq", it.id, it.question, (it.options + it.explanation).joinToString(" "))) }
        }

        /** Turns typed words into an FTS query: every word must appear, prefixes allowed. */
        fun query(text: String): String? {
            val words = text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }
            return if (words.isEmpty()) null else words.joinToString(" ") { "$it*" }
        }
    }
}
