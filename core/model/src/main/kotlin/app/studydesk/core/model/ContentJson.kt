package app.studydesk.core.model

import kotlinx.serialization.json.Json

/**
 * The one JSON setup for reading content. Unknown fields are ignored so a newer
 * file (same contentFormat, extra fields) still reads in an older app.
 */
val ContentJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/** The content format this app reads. Bump together with the content repo's CONTENT_FORMAT. */
const val SUPPORTED_CONTENT_FORMAT = 1
