package dev.bxane.rynx.android

/** Requests Android's own audio route picker; never redirects the PCM stream to a second player. */
internal object AudioOutputRouteRequest {
    /**
     * Uses the native output switcher on Android 14+ when the system presents it.
     * Devices that reject the request, and older versions, retain the existing settings fallback.
     */
    fun launch(sdk: Int, showNative: () -> Boolean, showFallback: () -> Unit): Boolean {
        val shown = sdk >= 34 && runCatching(showNative).getOrDefault(false)
        if (!shown) showFallback()
        return shown
    }
}
