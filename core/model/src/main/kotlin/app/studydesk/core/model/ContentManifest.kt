package app.studydesk.core.model

import kotlinx.serialization.Serializable

/** `v1/manifest.json`: what exists on the server, with each file's size and sha256. */
@Serializable
data class ContentManifest(
    val contentFormat: Int,
    val minAppVersion: Int,
    val generatedAt: String,
    val catalog: FileEntry,
    val exams: List<ExamFileEntry> = emptyList(),
    val packs: List<PackEntry> = emptyList(),
) {
    /** Every downloadable text file (catalog, date sheets, packs), without images. */
    val textFiles: List<FileEntry>
        get() = listOf(catalog) + exams.map { it.file } + packs.map { it.file }
}

@Serializable
data class FileEntry(
    val path: String,
    val bytes: Long,
    val sha256: String,
    val version: String,
)

@Serializable
data class ExamFileEntry(
    val bodyId: String,
    val path: String,
    val bytes: Long,
    val sha256: String,
    val version: String,
) {
    val file: FileEntry get() = FileEntry(path, bytes, sha256, version)
}

@Serializable
data class PackEntry(
    val id: String,
    val reviewed: Boolean,
    val path: String,
    val bytes: Long,
    val sha256: String,
    val version: String,
    val media: List<FileEntry> = emptyList(),
) {
    val file: FileEntry get() = FileEntry(path, bytes, sha256, version)
}
