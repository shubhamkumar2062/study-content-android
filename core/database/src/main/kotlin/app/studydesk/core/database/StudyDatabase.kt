package app.studydesk.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        McqAttemptEntity::class,
        MockResultEntity::class,
        CardStateEntity::class,
        SearchEntryEntity::class,
        SearchIndexStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun progress(): ProgressDao
    abstract fun bookmarks(): BookmarkDao
    abstract fun highlights(): HighlightDao
    abstract fun practice(): PracticeDao
    abstract fun cards(): CardDao
    abstract fun search(): SearchDao
}
