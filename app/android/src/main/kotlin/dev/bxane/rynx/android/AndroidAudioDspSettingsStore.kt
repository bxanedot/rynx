package dev.bxane.rynx.android

import android.content.Context
import android.content.SharedPreferences
import dev.bxane.rynx.playback.audio.AudioDspSettings
import dev.bxane.rynx.playback.audio.AudioEqualizerBand
import dev.bxane.rynx.playback.audio.AudioEqualizerLayout
import dev.bxane.rynx.playback.audio.AudioFilterShape
import org.json.JSONArray
import org.json.JSONObject

/** Device-local DSP preferences. No account or network service is involved. */
internal class AndroidAudioDspSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): AudioDspSettings = decode(preferences.getString(SETTINGS_KEY, null))

    fun save(settings: AudioDspSettings) {
        preferences.edit().putString(SETTINGS_KEY, encode(settings.normalized())).apply()
    }

    fun observe(onChanged: (AudioDspSettings) -> Unit): AutoCloseable {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
            if (key == SETTINGS_KEY) onChanged(decode(changed.getString(SETTINGS_KEY, null)))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return AutoCloseable { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun encode(settings: AudioDspSettings): String {
        val bands = JSONArray()
        settings.equalizerBands.forEach { band ->
            bands.put(JSONObject()
                .put("frequencyHz", band.frequencyHz.toDouble())
                .put("gainDb", band.gainDb.toDouble())
                .put("q", band.q.toDouble())
                .put("shape", band.shape.name)
                .put("enabled", band.enabled))
        }
        return JSONObject()
            .put("pureMode", settings.pureMode)
            .put("equalizerEnabled", settings.equalizerEnabled)
            .put("equalizerLayout", settings.equalizerLayout.name)
            .put("equalizerBands", bands)
            .put("bassDb", settings.bassDb.toDouble())
            .put("trebleDb", settings.trebleDb.toDouble())
            .put("preampDb", settings.preampDb.toDouble())
            .put("autoHeadroom", settings.autoHeadroom)
            .put("stereoWidth", settings.stereoWidth.toDouble())
            .put("stereoBalance", settings.stereoBalance.toDouble())
            .put("crossfeed", settings.crossfeed.toDouble())
            .put("compressorEnabled", settings.compressorEnabled)
            .put("compressorThresholdDb", settings.compressorThresholdDb.toDouble())
            .put("compressorRatio", settings.compressorRatio.toDouble())
            .put("compressorAttackMs", settings.compressorAttackMs.toDouble())
            .put("compressorReleaseMs", settings.compressorReleaseMs.toDouble())
            .put("limiterEnabled", settings.limiterEnabled)
            .put("limiterCeiling", settings.limiterCeiling.toDouble())
            .put("ditherEnabled", settings.ditherEnabled)
            .put("skipSilenceEnabled", settings.skipSilenceEnabled)
            .toString()
    }

    private fun decode(raw: String?): AudioDspSettings {
        if (raw.isNullOrBlank()) return AudioDspSettings()
        return runCatching {
            val json = JSONObject(raw)
            val layout = runCatching {
                AudioEqualizerLayout.valueOf(json.optString("equalizerLayout", AudioEqualizerLayout.TEN_BAND.name))
            }.getOrDefault(AudioEqualizerLayout.TEN_BAND)
            val defaults = AudioDspSettings.forLayout(layout)
            val stored = json.optJSONArray("equalizerBands")?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    val value = array.optJSONObject(index) ?: return@mapNotNull null
                    val shape = runCatching { AudioFilterShape.valueOf(value.optString("shape", AudioFilterShape.PEAKING.name)) }
                        .getOrDefault(AudioFilterShape.PEAKING)
                    AudioEqualizerBand(
                        frequencyHz = value.optDouble("frequencyHz", 1_000.0).toFloat(),
                        gainDb = value.optDouble("gainDb", 0.0).toFloat(),
                        q = value.optDouble("q", 1.0).toFloat(),
                        shape = shape,
                        enabled = value.optBoolean("enabled", true),
                    )
                }
            } ?: defaults.equalizerBands
            defaults.copy(
                pureMode = json.optBoolean("pureMode", false),
                equalizerEnabled = json.optBoolean("equalizerEnabled", false),
                equalizerBands = stored,
                bassDb = json.optDouble("bassDb", 0.0).toFloat(),
                trebleDb = json.optDouble("trebleDb", 0.0).toFloat(),
                preampDb = json.optDouble("preampDb", 0.0).toFloat(),
                autoHeadroom = json.optBoolean("autoHeadroom", true),
                stereoWidth = json.optDouble("stereoWidth", 1.0).toFloat(),
                stereoBalance = json.optDouble("stereoBalance", 0.0).toFloat(),
                crossfeed = json.optDouble("crossfeed", 0.0).toFloat(),
                compressorEnabled = json.optBoolean("compressorEnabled", false),
                compressorThresholdDb = json.optDouble("compressorThresholdDb", -18.0).toFloat(),
                compressorRatio = json.optDouble("compressorRatio", 2.0).toFloat(),
                compressorAttackMs = json.optDouble("compressorAttackMs", 10.0).toFloat(),
                compressorReleaseMs = json.optDouble("compressorReleaseMs", 120.0).toFloat(),
                limiterEnabled = json.optBoolean("limiterEnabled", false),
                limiterCeiling = json.optDouble("limiterCeiling", .98).toFloat(),
                ditherEnabled = json.optBoolean("ditherEnabled", false),
                skipSilenceEnabled = json.optBoolean("skipSilenceEnabled", false),
            ).normalized()
        }.getOrDefault(AudioDspSettings())
    }

    private companion object {
        const val PREFERENCES = "rynx_audio_dsp"
        const val SETTINGS_KEY = "settings_v1"
    }
}
