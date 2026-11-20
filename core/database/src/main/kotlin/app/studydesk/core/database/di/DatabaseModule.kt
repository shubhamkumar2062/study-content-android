package app.studydesk.core.database.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.studydesk.core.content.WantedPacks
import app.studydesk.core.database.StudyDatabase
import app.studydesk.core.database.UserPreferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): StudyDatabase =
        Room.databaseBuilder(context, StudyDatabase::class.java, "studydesk.db").build()

    @Provides fun progress(db: StudyDatabase) = db.progress()
    @Provides fun bookmarks(db: StudyDatabase) = db.bookmarks()
    @Provides fun highlights(db: StudyDatabase) = db.highlights()
    @Provides fun practice(db: StudyDatabase) = db.practice()
    @Provides fun cards(db: StudyDatabase) = db.cards()
    @Provides fun search(db: StudyDatabase) = db.search()

    @Provides
    @Singleton
    fun preferences(@ApplicationContext context: Context) =
        UserPreferences(PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") })
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WantedPacksModule {
    @Binds
    abstract fun wantedPacks(preferences: UserPreferences): WantedPacks
}
