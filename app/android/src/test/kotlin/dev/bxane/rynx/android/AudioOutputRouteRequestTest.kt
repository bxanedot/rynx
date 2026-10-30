package dev.bxane.rynx.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputRouteRequestTest {
    @Test fun modernAndroidShowsNativePickerWithoutOpeningSettings() {
        var nativeCalls = 0
        var fallbackCalls = 0
        val result = AudioOutputRouteRequest.launch(34, { nativeCalls++; true }, { fallbackCalls++ })
        assertTrue(result)
        assertEquals(1, nativeCalls)
        assertEquals(0, fallbackCalls)
    }

    @Test fun olderAndroidOpensSettingsAndNeverCallsUnsupportedNativeApi() {
        var nativeCalls = 0
        var fallbackCalls = 0
        val result = AudioOutputRouteRequest.launch(33, { nativeCalls++; true }, { fallbackCalls++ })
        assertFalse(result)
        assertEquals(0, nativeCalls)
        assertEquals(1, fallbackCalls)
    }

    @Test fun rejectedOrFailingNativePickerFallsBackSafely() {
        var fallbackCalls = 0
        assertFalse(AudioOutputRouteRequest.launch(34, { false }, { fallbackCalls++ }))
        assertFalse(AudioOutputRouteRequest.launch(35, { error("Native router unavailable") }, { fallbackCalls++ }))
        assertEquals(2, fallbackCalls)
    }
}
