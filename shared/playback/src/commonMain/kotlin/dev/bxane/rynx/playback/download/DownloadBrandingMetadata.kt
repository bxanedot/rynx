package dev.bxane.rynx.playback.download

/** Portable provenance record to write beside a completed download as `<media-file>.rynx.json`. */
data class DownloadBrandingMetadata(
    val mediaId: String,
    val title: String,
    val artist: String? = null,
    val provider: String? = null,
    val downloadedAtEpochMillis: Long,
) {
    val application: String get() = APPLICATION_NAME
    val applicationId: String get() = APPLICATION_ID
    val comment: String get() = ATTRIBUTION

    init {
        require(mediaId.isNotBlank()) { "mediaId must not be blank" }
        require(downloadedAtEpochMillis >= 0) { "downloadedAtEpochMillis must not be negative" }
    }

    /** Stable, privacy-conscious sidecar JSON; it never copies signed stream URLs or credentials. */
    fun toSidecarJson(): String = buildString {
        append('{')
        append("\"schemaVersion\":1,")
        append("\"application\":").append(APPLICATION_NAME.jsonQuoted()).append(',')
        append("\"applicationId\":").append(APPLICATION_ID.jsonQuoted()).append(',')
        append("\"comment\":").append(ATTRIBUTION.jsonQuoted()).append(',')
        append("\"mediaId\":").append(mediaId.jsonQuoted()).append(',')
        append("\"title\":").append(title.jsonQuoted()).append(',')
        append("\"artist\":").append(artist.jsonValue()).append(',')
        append("\"provider\":").append(provider.jsonValue()).append(',')
        append("\"downloadedAtEpochMillis\":").append(downloadedAtEpochMillis).append('}')
    }

    private fun String?.jsonValue(): String = this?.jsonQuoted() ?: "null"

    private fun String.jsonQuoted(): String = buildString(length + 2) {
        append('"')
        this@jsonQuoted.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000c' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) {
                    append("\\u")
                    append(char.code.toString(16).padStart(4, '0'))
                } else append(char)
            }
        }
        append('"')
    }

    private companion object {
        const val APPLICATION_NAME = "Rynx"
        const val APPLICATION_ID = "dev.bxane.rynx"
        const val ATTRIBUTION = "Downloaded by Rynx"
    }
}
