package app.studydesk.core.content

import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.ContentManifest
import app.studydesk.core.model.ExamFileEntry
import app.studydesk.core.model.FileEntry
import app.studydesk.core.model.PackEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.net.UnknownHostException

private const val PRIMARY = "https://pages.test/v1/"
private const val MIRROR = "https://mirror.test/v1/"
private const val HOUR = 60 * 60 * 1000L

class ContentSyncerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val server = FakeServer()
    private var now = 1_000_000_000L
    private var freeSpace = Long.MAX_VALUE
    private var wanted = setOf("a")
    private lateinit var bundle: FakeBundle
    private lateinit var storage: ContentStorage
    private lateinit var syncer: ContentSyncer

    @Before
    fun setUp() {
        // Version 1 of everything is both on the server and inside the app.
        server.put("catalog.json", "catalog-1")
        server.put("exams/x.json", "exams-1")
        server.put("packs/a.json", "pack-a-1")
        server.put("packs/b.json", "pack-b-1")
        server.publish()
        bundle = FakeBundle(server.manifest, server.files.toMap())
        storage = ContentStorage(tmp.newFolder("content")) { freeSpace }
        syncer = newSyncer(listOf(PRIMARY, MIRROR))
    }

    private fun newSyncer(urls: List<String>) = ContentSyncer(
        source = HttpContentSource(HttpClient(server.engine), urls),
        storage = storage,
        bundled = bundle,
        config = ContentConfig(urls, appContentVersion = 1, minCheckIntervalMillis = 6 * HOUR),
        wantedPacks = { wanted },
        clock = { now },
        io = Dispatchers.Unconfined,
    )

    private fun readText(path: String): String? {
        val entry = syncer.currentState()!!.active.textFiles.first { it.path == path }
        return syncer.read(entry)?.decodeToString()
    }

    @Test
    fun freshInstallReadsTheBundleWithoutAnyRequest() {
        assertEquals("pack-a-1", readText("packs/a.json"))
        assertEquals(0, server.requests.size)
    }

    @Test
    fun unchangedServerCostsOneManifestRequest() = runTest {
        assertEquals(SyncResult.UpToDate, syncer.sync())   // first check: 200 with the manifest
        now += 7 * HOUR
        assertEquals(SyncResult.UpToDate, syncer.sync())   // second: ETag matches, 304
        assertEquals(listOf("${PRIMARY}manifest.json", "${PRIMARY}manifest.json"), server.requests)
        assertEquals(HttpStatusCode.NotModified, server.statuses.last())
    }

    @Test
    fun checksWithinSixHoursMakeNoRequestUnlessForced() = runTest {
        syncer.sync()
        now += 2 * HOUR
        assertEquals(SyncResult.Skipped, syncer.sync())
        assertEquals(1, server.requests.size)
        syncer.sync(force = true)
        assertEquals(2, server.requests.size)
    }

    @Test
    fun onlyTheChangedWantedPackIsDownloaded() = runTest {
        syncer.sync()
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        now += 7 * HOUR
        server.requests.clear()

        val result = syncer.sync()

        assertTrue(result is SyncResult.Updated)
        assertEquals(listOf("${PRIMARY}manifest.json", "${PRIMARY}packs/a.json"), server.requests)
        assertEquals("pack-a-2", readText("packs/a.json"))
    }

    @Test
    fun aChangedPackTheStudentDoesNotNeedWaitsUntilOpened() = runTest {
        server.put("packs/b.json", "pack-b-2")
        server.publish()

        syncer.sync()

        assertEquals("pack-b-1", readText("packs/b.json"))           // old copy still readable
        assertEquals(server.entry("packs/b.json").sha256, syncer.currentState()!!.newerPack("b")!!.sha256)
        assertEquals(1, server.requests.size)

        assertTrue(syncer.downloadPack("b") is SyncResult.Updated)
        assertEquals("pack-b-2", readText("packs/b.json"))
        assertNull(syncer.currentState()!!.newerPack("b"))
        assertTrue("b" in syncer.currentState()!!.pinned)
    }

    @Test
    fun wrongChecksumKeepsTheOldContent() = runTest {
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        server.corrupt = "packs/a.json"

        val result = syncer.sync()

        assertEquals(SyncResult.Failed(SyncFailure.Corrupt("packs/a.json")), result)
        assertEquals("pack-a-1", readText("packs/a.json"))
        assertNull(storage.readState())   // nothing committed
    }

    @Test
    fun interruptedDownloadKeepsTheOldContentAndRetriesLater() = runTest {
        server.put("catalog.json", "catalog-2")
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        server.failOnPath = "packs/a.json"

        assertTrue(syncer.sync() is SyncResult.Failed)
        assertEquals("catalog-1", readText("catalog.json"))
        assertEquals("pack-a-1", readText("packs/a.json"))

        // Not marked as checked, so the next try does not wait six hours.
        server.failOnPath = null
        server.requests.clear()
        assertTrue(syncer.sync() is SyncResult.Updated)
        assertEquals("catalog-2", readText("catalog.json"))
        assertEquals("pack-a-2", readText("packs/a.json"))
        // The catalog arrived the first time and is not downloaded again.
        assertTrue(server.requests.none { it.endsWith("catalog.json") })
    }

    @Test
    fun theBackupIsUsedOnlyWhenTheMainServerFails() = runTest {
        syncer.sync()
        assertTrue(server.requests.none { it.startsWith(MIRROR) })

        server.primaryDown = true
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        now += 7 * HOUR
        server.requests.clear()

        assertTrue(syncer.sync() is SyncResult.Updated)
        assertEquals("pack-a-2", readText("packs/a.json"))
        assertEquals(
            listOf("${PRIMARY}manifest.json", "${MIRROR}manifest.json", "${PRIMARY}packs/a.json", "${MIRROR}packs/a.json"),
            server.requests,
        )
    }

    @Test
    fun offlinePhoneFailsFastWithoutTryingTheBackup() = runTest {
        server.offline = true
        assertEquals(SyncResult.Failed(SyncFailure.Offline), syncer.sync())
        assertEquals(1, server.requests.size)
        assertEquals("pack-a-1", readText("packs/a.json"))
    }

    @Test
    fun contentForANewerAppIsNotDownloaded() = runTest {
        server.put("packs/a.json", "pack-a-2")
        server.minAppVersion = 2
        server.publish()

        assertEquals(SyncResult.UpdateRequired, syncer.sync())
        assertEquals(1, server.requests.size)
        assertEquals("pack-a-1", readText("packs/a.json"))
        assertTrue(syncer.currentState()!!.appUpdateRequired)
    }

    @Test
    fun fullStorageIsReportedAndNothingChanges() = runTest {
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        freeSpace = 1024

        val result = syncer.sync()

        assertTrue(result is SyncResult.Failed && result.failure is SyncFailure.StorageFull)
        assertEquals(1, server.requests.size)
        assertEquals("pack-a-1", readText("packs/a.json"))
    }

    @Test
    fun replacedFilesAreDeletedFromThePhone() = runTest {
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        syncer.sync()
        val v2 = server.entry("packs/a.json").sha256

        server.put("packs/a.json", "pack-a-3")
        server.publish()
        now += 7 * HOUR
        syncer.sync()

        assertTrue(storage.hasObject(server.entry("packs/a.json").sha256))
        assertTrue(!storage.hasObject(v2))
    }

    @Test
    fun imagesDownloadOnlyWhenAsked() = runTest {
        server.put("media/a/ring.svg", "<svg/>")
        server.mediaOf["a"] = listOf("media/a/ring.svg")
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        syncer.sync()
        assertTrue(server.requests.none { it.contains("media/") })

        assertTrue(syncer.downloadMedia("a") is SyncResult.Updated)
        val entry = syncer.currentState()!!.active.packs.first { it.id == "a" }.media.single()
        assertArrayEquals("<svg/>".encodeToByteArray(), syncer.mediaFile(entry)!!.readBytes())
        assertEquals(SyncResult.UpToDate, syncer.downloadMedia("a"))
    }

    @Test
    fun appUpdateWithANewerBundleIsUsedWithoutGoingOnline() {
        // The phone saved content from November 1st; the updated app ships content from November 18th.
        storage.writeState(LocalState(active = server.manifest.copy(generatedAt = "2026-11-01T10:00:00+05:30"), etag = "\"1\""))
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        val newer = server.manifest.copy(generatedAt = "2026-11-18T18:32:27+05:30")
        bundle = FakeBundle(newer, server.files.toMap())
        syncer = newSyncer(listOf(PRIMARY, MIRROR))

        assertEquals("pack-a-2", readText("packs/a.json"))
        assertEquals("\"1\"", syncer.currentState()!!.etag)   // the rest of the saved state is kept
        assertEquals(0, server.requests.size)
    }

    @Test
    fun anOlderBundleNeverReplacesNewerSavedContent() {
        server.put("packs/a.json", "pack-a-2")
        server.publish()
        storage.writeState(LocalState(active = server.manifest.copy(generatedAt = "2026-11-18T13:02:27Z")))
        bundle = FakeBundle(bundle.manifestOrThrow().copy(generatedAt = "2026-11-18T18:00:00+05:30"), bundle.files)   // 12:30 UTC, before 13:02 UTC
        syncer = newSyncer(listOf(PRIMARY, MIRROR))
        storage.writeObject(server.entry("packs/a.json").sha256, server.files.getValue("packs/a.json"))

        assertEquals("pack-a-2", readText("packs/a.json"))
    }
}

private class FakeBundle(private val manifest: ContentManifest, val files: Map<String, ByteArray>) : BundledContent {
    override fun manifest() = manifest
    fun manifestOrThrow() = manifest
    override fun read(path: String) = files[path]
}

private class FakeServer {
    val files = linkedMapOf<String, ByteArray>()
    val mediaOf = mutableMapOf<String, List<String>>()
    var minAppVersion = 1
    lateinit var manifest: ContentManifest
    private var etag = ""
    private var version = 0

    val requests = mutableListOf<String>()
    val statuses = mutableListOf<HttpStatusCode>()
    var primaryDown = false
    var offline = false
    var failOnPath: String? = null
    var corrupt: String? = null

    fun put(path: String, text: String) {
        files[path] = text.encodeToByteArray()
    }

    fun entry(path: String): FileEntry {
        val b = files.getValue(path)
        val sha = sha256(b)
        return FileEntry(path, b.size.toLong(), sha, sha.take(16))
    }

    fun publish() {
        val packs = files.keys.filter { it.startsWith("packs/") }.map { path ->
            val e = entry(path)
            val id = path.removePrefix("packs/").removeSuffix(".json")
            PackEntry(id, true, e.path, e.bytes, e.sha256, e.version, mediaOf[id].orEmpty().map(::entry))
        }
        val exams = files.keys.filter { it.startsWith("exams/") }.map { path ->
            val e = entry(path)
            ExamFileEntry(path.substringAfter('/').removeSuffix(".json"), e.path, e.bytes, e.sha256, e.version)
        }
        manifest = ContentManifest(1, minAppVersion, "v${++version}", entry("catalog.json"), exams, packs)
        files["manifest.json"] = ContentJson.encodeToString(ContentManifest.serializer(), manifest).encodeToByteArray()
        etag = "\"$version\""
    }

    val engine = MockEngine { request ->
        val url = request.url.toString()
        requests += url
        if (offline) throw UnknownHostException("pages.test")
        val primary = url.startsWith(PRIMARY)
        val path = url.removePrefix(if (primary) PRIMARY else MIRROR)
        fun reply(status: HttpStatusCode, body: ByteArray = ByteArray(0)) =
            respond(body, status, headersOf(HttpHeaders.ETag, etag)).also { statuses += status }
        when {
            primary && primaryDown -> reply(HttpStatusCode.ServiceUnavailable)
            path == failOnPath -> throw IOException("connection reset")
            path == "manifest.json" && primary && request.headers[HttpHeaders.IfNoneMatch] == etag -> reply(HttpStatusCode.NotModified)
            files[path] == null -> reply(HttpStatusCode.NotFound)
            path == corrupt -> reply(HttpStatusCode.OK, files.getValue(path) + byteArrayOf(0))
            else -> reply(HttpStatusCode.OK, files.getValue(path))
        }
    }
}
