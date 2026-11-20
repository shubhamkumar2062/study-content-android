package app.studydesk.core.content

/**
 * Where content comes from and how often the app may ask. Provided by the app module.
 *
 * @param baseUrls the main address first, then backups (each ends with `/v1/`)
 * @param appContentVersion compared with the manifest's `minAppVersion`
 * @param minCheckIntervalMillis at most one manifest request per this interval (unless forced)
 */
data class ContentConfig(
    val baseUrls: List<String>,
    val appContentVersion: Int,
    val minCheckIntervalMillis: Long = 6 * 60 * 60 * 1000L,
    /** Debug builds log every request so the number of server hits can be checked. */
    val logRequests: Boolean = false,
) {
    init {
        require(baseUrls.isNotEmpty()) { "At least one content address is needed" }
        require(baseUrls.all { it.endsWith("/") }) { "Content addresses must end with /" }
    }
}

fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}
