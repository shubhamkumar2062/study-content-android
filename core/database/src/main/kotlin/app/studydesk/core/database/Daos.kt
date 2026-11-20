package app.studydesk.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE packId = :packId AND docId = :docId")
    suspend fun get(packId: String, docId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE packId = :packId")
    fun forPack(packId: String): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress ORDER BY updatedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress")
    fun all(): Flow<List<ReadingProgressEntity>>

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun all(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE packId = :packId")
    fun forPack(packId: String): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE packId = :packId AND kind = :kind AND targetId = :targetId")
    suspend fun delete(packId: String, kind: String, targetId: String): Int
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights ORDER BY updatedAt DESC")
    fun all(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE packId = :packId AND docId = :docId ORDER BY blockIndex, start")
    fun forDoc(packId: String, docId: String): Flow<List<HighlightEntity>>

    @Insert
    suspend fun insert(highlight: HighlightEntity): Long

    @Query("UPDATE highlights SET note = :note, colorIndex = :colorIndex, updatedAt = :at WHERE id = :id")
    suspend fun update(id: Long, note: String?, colorIndex: Int, at: Long)

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun get(id: Long): HighlightEntity?

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert
    suspend fun restore(highlight: HighlightEntity)
}

/** Totals for one subject. */
data class PackAccuracy(val packId: String, val attempts: Int, val correct: Int)

data class PackCount(val packId: String, val count: Int)

/** Correct answers / attempts for one unit. */
data class UnitAccuracy(val unit: Int, val attempts: Int, val correct: Int)

@Dao
interface PracticeDao {
    @Insert
    suspend fun insertAttempt(attempt: McqAttemptEntity)

    @Query(
        """SELECT unit, COUNT(*) AS attempts, SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct
           FROM (SELECT * FROM mcq_attempts WHERE packId = :packId
                 AND id IN (SELECT MAX(id) FROM mcq_attempts WHERE packId = :packId GROUP BY mcqId))
           GROUP BY unit ORDER BY unit""",
    )
    fun accuracyByUnit(packId: String): Flow<List<UnitAccuracy>>

    /** MCQs whose latest answer was wrong. */
    @Query(
        """SELECT mcqId FROM mcq_attempts WHERE packId = :packId AND id IN
           (SELECT MAX(id) FROM mcq_attempts WHERE packId = :packId GROUP BY mcqId) AND correct = 0""",
    )
    fun wrongMcqIds(packId: String): Flow<List<String>>

    @Query(
        """SELECT packId, COUNT(*) AS attempts, SUM(CASE WHEN correct THEN 1 ELSE 0 END) AS correct
           FROM mcq_attempts WHERE id IN (SELECT MAX(id) FROM mcq_attempts GROUP BY packId, mcqId)
           GROUP BY packId""",
    )
    fun accuracyByPack(): Flow<List<PackAccuracy>>

    @Query("SELECT packId, COUNT(*) AS count FROM mock_results GROUP BY packId")
    fun mockCounts(): Flow<List<PackCount>>

    @Insert
    suspend fun insertResult(result: MockResultEntity): Long

    @Query("SELECT * FROM mock_results WHERE packId = :packId ORDER BY finishedAt DESC")
    fun results(packId: String): Flow<List<MockResultEntity>>

    @Query("SELECT * FROM mock_results WHERE id = :id")
    suspend fun result(id: Long): MockResultEntity?
}

@Dao
interface CardDao {
    @Query("SELECT * FROM card_states WHERE packId = :packId")
    fun forPack(packId: String): Flow<List<CardStateEntity>>

    @Query("SELECT COUNT(*) FROM card_states WHERE dueAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Upsert
    suspend fun upsert(state: CardStateEntity)

    @Query("DELETE FROM card_states WHERE packId = :packId AND questionId = :questionId")
    suspend fun delete(packId: String, questionId: String)

    @Query("SELECT * FROM card_states WHERE dueAt <= :now ORDER BY dueAt")
    suspend fun due(now: Long): List<CardStateEntity>
}

/** One search result row. */
data class SearchHit(
    val packId: String,
    val subjectTitle: String,
    val kind: String,
    val refId: String,
    val title: String,
    val snippet: String,
)

@Dao
interface SearchDao {
    @Query(
        """SELECT packId, subjectTitle, kind, refId, title,
           snippet(search_index, char(2), char(3), '…', -1, 12) AS snippet
           FROM search_index WHERE search_index MATCH :query LIMIT :limit""",
    )
    suspend fun search(query: String, limit: Int = 60): List<SearchHit>

    @Query("SELECT * FROM search_index_state")
    suspend fun indexed(): List<SearchIndexStateEntity>

    @Query("DELETE FROM search_index WHERE packId = :packId")
    suspend fun deleteEntries(packId: String)

    @Insert
    suspend fun insertEntries(entries: List<SearchEntryEntity>)

    @Upsert
    suspend fun markIndexed(state: SearchIndexStateEntity)

    @Query("DELETE FROM search_index_state WHERE packId = :packId")
    suspend fun unmark(packId: String)

    @Transaction
    suspend fun replace(packId: String, sha256: String, entries: List<SearchEntryEntity>) {
        deleteEntries(packId)
        insertEntries(entries)
        markIndexed(SearchIndexStateEntity(packId, sha256))
    }
}
