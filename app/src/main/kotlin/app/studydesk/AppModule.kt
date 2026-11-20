package app.studydesk

import app.studydesk.core.content.ContentConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun contentConfig() = ContentConfig(
        baseUrls = listOf(BuildConfig.CONTENT_BASE_URL, BuildConfig.CONTENT_MIRROR_URL),
        appContentVersion = BuildConfig.CONTENT_APP_VERSION,
        logRequests = BuildConfig.DEBUG,
    )

    /** Whether subjects a teacher hasn't checked yet can be opened (they are always labelled). */
    @Provides
    @Named("showUnreviewed")
    fun showUnreviewed(): Boolean = BuildConfig.SHOW_UNREVIEWED
}
