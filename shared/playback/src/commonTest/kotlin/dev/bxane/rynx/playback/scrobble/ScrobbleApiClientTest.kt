package dev.bxane.rynx.playback.scrobble

import dev.bxane.rynx.playback.runSuspend
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScrobbleApiClientTest {
    @Test fun lastFmSignatureUsesMd5AndSortedParameters() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", rynxMd5Hex(""))
        assertEquals("900150983cd24fb0d6963f7d28e17f72", rynxMd5Hex("abc"))
        assertEquals(
            rynxMd5Hex("api_keykeyartistnamemethodtrack.scrobblesksecrettimestamp123tracknamesecret"),
            lastFmApiSignature(
                mapOf("track" to "name", "format" to "json", "api_sig" to "ignored", "artist" to "name", "method" to "track.scrobble", "api_key" to "key", "sk" to "secret", "timestamp" to "123"),
                "secret",
            ),
        )
    }

    @Test fun lastFmBrowserApprovalUsesTokenThenExchangesItForSession() = runSuspend {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = HttpClient(MockEngine { request ->
            requests += request
            val response = if (request.url.parameters["method"] == "auth.getToken") {
                """{"token":"temporary-token"}"""
            } else {
                """{"session":{"name":"bxane","key":"session-key","subscriber":0}}"""
            }
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val lastFm = LastFmClient(client, "app-key", "app-secret")
            val token = lastFm.requestAuthorizationToken()
            assertEquals("temporary-token", token)
            assertTrue(lastFm.authorizationUrl(token).contains("api_key=app-key"))
            assertTrue(lastFm.authorizationUrl(token).contains("token=temporary-token"))
            val session = lastFm.completeAuthorization(token)
            assertEquals("bxane", session.username)
            assertEquals("session-key", session.sessionKey)
            assertEquals("auth.getToken", requests.first().url.parameters["method"])
            assertEquals("app-key", requests.first().url.parameters["api_key"])
            val form = requests.last().body as FormDataContent
            assertEquals("auth.getSession", form.formData["method"])
            assertEquals("temporary-token", form.formData["token"])
            assertTrue(form.formData["api_sig"].orEmpty().isNotBlank())
            assertTrue(requests.none { it.url.parameters["password"] != null })

            lastFm.scrobble(session, ScrobbleTrackMetadata("Blue Hour", "Rynx", "Night Drive", 181_000), 1_800_000_000L)
            val scrobbleForm = (requests.last().body as FormDataContent).formData
            assertEquals("track.scrobble", scrobbleForm["method"])
            assertEquals("Blue Hour", scrobbleForm["track"])
            assertEquals("Night Drive", scrobbleForm["album"])
            assertEquals("181", scrobbleForm["duration"])
            assertEquals("1800000000", scrobbleForm["timestamp"])
            assertEquals("session-key", scrobbleForm["sk"])
            val signedValues = scrobbleForm.entries().associate { it.key to it.value.first() }
            assertEquals(
                lastFmApiSignature(signedValues - "api_sig", "app-secret"),
                scrobbleForm["api_sig"],
            )
        } finally {
            client.close()
        }
    }

    @Test fun listenBrainzValidatesTokenAndSubmitsRealListenPayload() = runSuspend {
        val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()
        val client = HttpClient(MockEngine { request ->
            requests += request
            val response = if (request.url.encodedPath.endsWith("validate-token")) {
                """{"valid":true,"user_name":"bxane"}"""
            } else """{"status":"ok"}"""
            respond(response, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })
        try {
            val listenBrainz = ListenBrainzClient(client)
            val account = listenBrainz.validateToken("private-token")
            assertEquals("bxane", account.username)
            val track = ScrobbleTrackMetadata("Blue Hour", "Rynx", "Night Drive", 240_000)
            listenBrainz.submitListen("private-token", track, 1_800_000_000L)
            val validation = requests.first()
            assertEquals("Token private-token", validation.headers[HttpHeaders.Authorization])
            val submission = requests.last()
            assertEquals("POST", submission.method.value)
            assertEquals("Token private-token", submission.headers[HttpHeaders.Authorization])
            assertTrue(submission.headers["User-Agent"].orEmpty().contains("rynx"))
            val payload = (submission.body as io.ktor.http.content.TextContent).text
            val json = Json.parseToJsonElement(payload).jsonObject
            assertEquals("single", json["listen_type"]?.jsonPrimitive?.content)
            assertEquals(1_800_000_000L, json["payload"]?.jsonArray?.single()?.jsonObject?.get("listened_at")?.jsonPrimitive?.content?.toLong())
            val metadata = json["payload"]?.jsonArray?.single()?.jsonObject?.get("track_metadata")?.jsonObject
            assertEquals("Blue Hour", metadata?.get("track_name")?.jsonPrimitive?.content)
            assertEquals("Rynx", metadata?.get("artist_name")?.jsonPrimitive?.content)
            listenBrainz.updateNowPlaying("private-token", track)
            val nowPlayingPayload = Json.parseToJsonElement((requests.last().body as TextContent).text).jsonObject
            assertEquals("playing_now", nowPlayingPayload["listen_type"]?.jsonPrimitive?.content)
        } finally {
            client.close()
        }
    }

    @Test fun listenBrainzRejectsAnInvalidToken() {
        runSuspend {
            val client = HttpClient(MockEngine {
                respond("""{"valid":false}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            })
            try {
                assertFailsWith<ScrobbleAuthenticationException> { ListenBrainzClient(client).validateToken("bad") }
            } finally {
                client.close()
            }
        }
    }
}
