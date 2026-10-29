package dev.bxane.rynx.playback.audio

/** Best-effort source facts reported by the platform player. Output DAC format may remain OS-managed. */
data class AudioSignalInfo(
    val sourceSampleRateHz: Int? = null,
    val sourceBitDepth: Int? = null,
    val sourceEncoding: String? = null,
    val primaryOutputNativeRateHz: Int? = null,
    val availableOutputRoutes: List<String> = emptyList(),
)
