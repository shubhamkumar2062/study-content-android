package app.studydesk.core.content

import android.content.res.AssetManager
import app.studydesk.core.model.ContentJson
import app.studydesk.core.model.ContentManifest
import java.io.FileNotFoundException

/** The copy of `v1/` shipped inside the app (text only), so a fresh install works offline. */
interface BundledContent {
    fun manifest(): ContentManifest?
    fun read(path: String): ByteArray?
}

class AssetBundledContent(
    private val assets: AssetManager,
    private val root: String = "content/v1",
) : BundledContent {
    private val manifest: ContentManifest? by lazy {
        read("manifest.json")?.let { runCatching { ContentJson.decodeFromString<ContentManifest>(it.decodeToString()) }.getOrNull() }
    }

    override fun manifest(): ContentManifest? = manifest

    override fun read(path: String): ByteArray? = try {
        assets.open("$root/$path").use { it.readBytes() }
    } catch (_: FileNotFoundException) {
        null
    }
}
