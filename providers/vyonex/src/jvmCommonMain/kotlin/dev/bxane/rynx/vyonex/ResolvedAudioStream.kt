package dev.bxane.rynx.vyonex

/** Short-lived VyonEx resolution. Keep the URL and request headers in memory and out of logs/state persistence. */
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
            "profileId=$profileId, headers=${headers.keys})"
}
