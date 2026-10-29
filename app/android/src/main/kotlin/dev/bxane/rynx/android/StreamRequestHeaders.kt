package dev.bxane.rynx.android

/** Keeps short-lived provider request headers in memory for Media3's synchronous data-source hook. */
internal class StreamRequestHeaders(maxEntries: Int = DEFAULT_CAPACITY) {
    private val capacity = maxEntries.also { require(it > 0) }
    private val entries = LinkedHashMap<String, Map<String, String>>(16, 0.75f, true)

    @Synchronized
    fun register(uri: String, headers: Map<String, String>) {
        require(uri.isNotBlank())
        entries[uri] = headers.toMap()
        while (entries.size > capacity) entries.entries.iterator().run { next(); remove() }
    }

    @Synchronized
    fun headersFor(uri: String): Map<String, String> = entries[uri].orEmpty()

    @Synchronized
    fun clear() = entries.clear()

    private companion object {
        const val DEFAULT_CAPACITY = 32
    }
}

/** Shared by the activity's resolver and the process-owned media service. */
internal object StreamRequestHeaderStore {
    private val registry = StreamRequestHeaders()

    fun register(uri: String, headers: Map<String, String>) = registry.register(uri, headers)
    fun headersFor(uri: String): Map<String, String> = registry.headersFor(uri)
    fun clear() = registry.clear()
}
