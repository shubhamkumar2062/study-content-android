package app.studydesk.core.content

import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.ContentManifest
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The phone's own copy of content.
 *
 * Files are stored by checksum (`objects/<sha256>`), so a file that has not changed is
 * never downloaded twice. `state.json` says which files are in use. It is replaced with
 * an atomic rename, so the app always sees either the old set or the new set, never a mix.
 */
class ContentStorage(
    private val root: File,
    /** Free bytes on the phone; replaceable in tests. */
    private val freeSpaceOf: (File) -> Long = { it.usableSpace },
) {
    private val objects = File(root, "objects")
    private val stateFile = File(root, "state.json")

    fun readState(): LocalState? = runCatching {
        if (!stateFile.exists()) null else ContentJson.decodeFromString<LocalState>(stateFile.readText())
    }.getOrNull()

    fun writeState(state: LocalState) {
        root.mkdirs()
        atomicWrite(stateFile, ContentJson.encodeToString(LocalState.serializer(), state).encodeToByteArray())
    }

    fun hasObject(sha256: String): Boolean = File(objects, sha256).isFile

    fun readObject(sha256: String): ByteArray? = File(objects, sha256).takeIf { it.isFile }?.readBytes()

    fun objectFile(sha256: String): File? = File(objects, sha256).takeIf { it.isFile }

    fun writeObject(sha256: String, bytes: ByteArray) {
        objects.mkdirs()
        atomicWrite(File(objects, sha256), bytes)
    }

    /** Deletes stored files that nothing refers to any more. */
    fun deleteObjectsExcept(keep: Set<String>): Int {
        val files = objects.listFiles() ?: return 0
        var deleted = 0
        for (f in files) {
            if (f.name !in keep && f.delete()) deleted++
        }
        return deleted
    }

    fun objectsSize(): Long = objects.listFiles()?.sumOf { it.length() } ?: 0L

    fun freeSpace(): Long {
        root.mkdirs()
        return freeSpaceOf(root)
    }

    private fun atomicWrite(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.outputStream().use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }
}

/**
 * @param active the files the app reads now (every one is on the phone)
 * @param remote the newest manifest seen on the server; packs newer there than in
 *   [active] are "update available" and download when opened
 * @param pinned packs downloaded on purpose, kept up to date automatically
 */
@Serializable
data class LocalState(
    val active: ContentManifest,
    val remote: ContentManifest? = null,
    val etag: String? = null,
    val lastCheckedAt: Long? = null,
    val lastUpdatedAt: Long? = null,
    val appUpdateRequired: Boolean = false,
    val pinned: Set<String> = emptySet(),
)
