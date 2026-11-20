package app.studydesk.core.database

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** Where the student is in one document (a unit, extra or practical) and whether it is done. */
@Entity(tableName = "reading_progress", primaryKeys = ["packId", "docId"])
data class ReadingProgressEntity(
    val packId: String,
    val docId: String,
    val blockIndex: Int,
    val blockOffset: Int,
    val fraction: Float,
    val done: Boolean,
    val updatedAt: Long,
    val title: String,
    val subjectTitle: String,
)

/** A saved document, heading, question or MCQ. */
@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["packId", "kind", "targetId"], unique = true)],
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    /** "doc", "section", "question" or "mcq". */
    val kind: String,
    val targetId: String,
    val docId: String?,
    val blockIndex: Int?,
    val title: String,
    val subjectTitle: String,
    val createdAt: Long,
)

/** A highlighted passage, optionally with the student's own note. */
@Entity(tableName = "highlights", indices = [Index(value = ["packId", "docId"])])
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val docId: String,
    val blockIndex: Int,
    val start: Int,
    val end: Int,
    val colorIndex: Int,
    val quote: String,
    val note: String?,
    val docTitle: String,
    val subjectTitle: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "mcq_attempts", indices = [Index(value = ["packId", "unit"])])
data class McqAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val mcqId: String,
    val unit: Int,
    val chosen: Int,
    val correct: Boolean,
    val at: Long,
)

@Entity(tableName = "mock_results", indices = [Index(value = ["packId"])])
data class MockResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val testId: String,
    val testTitle: String,
    val startedAt: Long,
    val finishedAt: Long,
    val autoMarks: Int,
    val autoMax: Int,
    val selfMarks: Int,
    val selfMax: Int,
    val totalMarks: Int,
    /** JSON with the questions shown and the answers given, so a result can be reviewed later. */
    val detailJson: String,
)

/**
 * Flashcard schedule for one question (Leitner boxes 1-5). "Revise again" puts a card
 * back in box 1, due straight away; each "I know it" moves it up and further out.
 */
@Entity(tableName = "card_states", primaryKeys = ["packId", "questionId"])
data class CardStateEntity(
    val packId: String,
    val questionId: String,
    val box: Int,
    val dueAt: Long,
    val reviewedAt: Long,
)

/** Full-text search over everything saved on the phone. */
@Fts4
@Entity(tableName = "search_index")
data class SearchEntryEntity(
    val packId: String,
    val subjectTitle: String,
    /** "note", "question", "mcq" or "practical". */
    val kind: String,
    val refId: String,
    val title: String,
    val body: String,
)

/** Which pack version the search index was built from, so it is rebuilt only after a change. */
@Entity(tableName = "search_index_state")
data class SearchIndexStateEntity(
    @PrimaryKey val packId: String,
    val sha256: String,
)
