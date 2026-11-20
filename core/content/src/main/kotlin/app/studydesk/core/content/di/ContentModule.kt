package app.studydesk.core.content.di

import android.content.Context
import android.util.Log
import app.studydesk.core.content.AssetBundledContent
import app.studydesk.core.content.BundledContent
import app.studydesk.core.content.Clock
import app.studydesk.core.content.ContentConfig
import app.studydesk.core.content.ContentRepository
import app.studydesk.core.content.ContentSource
import app.studydesk.core.content.ContentStorage
import app.studydesk.core.content.ContentSyncer
import app.studydesk.core.content.HttpContentSource
import app.studydesk.core.content.WantedPacks
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import java.io.File
import javax.inject.Singleton

/** [ContentConfig] and [WantedPacks] are provided by the app and the database module. */
@Module
@InstallIn(SingletonComponent::class)
object ContentModule {

    @Provides
    @Singleton
    fun httpClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
            requestTimeoutMillis = 60_000
        }
    }

    @Provides
    @Singleton
    fun contentSource(client: HttpClient, config: ContentConfig): ContentSource =
        HttpContentSource(client, config.baseUrls) { if (config.logRequests) Log.d("StudyDeskSync", it) }

    @Provides
    @Singleton
    fun storage(@ApplicationContext context: Context) = ContentStorage(File(context.filesDir, "content"))

    @Provides
    @Singleton
    fun bundled(@ApplicationContext context: Context): BundledContent = AssetBundledContent(context.assets)

    @Provides
    @Singleton
    fun syncer(
        source: ContentSource,
        storage: ContentStorage,
        bundled: BundledContent,
        config: ContentConfig,
        wantedPacks: WantedPacks,
    ) = ContentSyncer(source, storage, bundled, config, wantedPacks, Clock.System)

    @Provides
    @Singleton
    fun repository(syncer: ContentSyncer) = ContentRepository(syncer)
}
