package dev.bxane.rynx.playback.catalog

/** Short-lived provider resolution. The signed URL and headers stay out of logs and persistence. */
class ResolvedAudioStream(
    val videoId: String,
    val url: String,
    val headers: Map<String, String>,
    val bitrateKbps: Int,
    val mimeType: String,
    val profileId: String,
) {
    override fun toString(): String =
        "ResolvedAudioStream(videoId=$videoId, bitrateKbps=$bitrateKbps, mimeType=$mimeType, " +
            "profileId=$profileId, hasRequestHeaders=${headers.isNotEmpty()})"
}
