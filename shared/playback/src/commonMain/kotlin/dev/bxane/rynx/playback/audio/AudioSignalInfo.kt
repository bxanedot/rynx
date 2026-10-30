package dev.bxane.rynx.playback.audio

/** Best-effort source facts reported by the platform player. Output DAC format may remain OS-managed. */
data class AudioSignalInfo(
    val sourceSampleRateHz: Int? = null,
    val sourceBitDepth: Int? = null,
    val sourceEncoding: String? = null,
    val primaryOutputNativeRateHz: Int? = null,
    val availableOutputRoutes: List<String> = emptyList(),
    /** Format actually passed to Rynx's Media3 PCM hook, not the source or physical DAC format. */
    val dspInputSampleRateHz: Int? = null,
    val dspInputBitDepth: Int? = null,
    val dspInputChannelCount: Int? = null,
    val dspProcessingActive: Boolean? = null,
)
