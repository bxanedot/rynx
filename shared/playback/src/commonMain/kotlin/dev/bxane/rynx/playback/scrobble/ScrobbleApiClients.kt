package dev.bxane.rynx.playback.scrobble

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

class LastFmClient(
    private val http: HttpClient,
    private val apiKey: String,
    private val sharedSecret: String,
) {
    val configured: Boolean get() = apiKey.isNotBlank() && sharedSecret.isNotBlank()

    suspend fun requestAuthorizationToken(): String {
        ensureConfigured()
        val response = http.get(LAST_FM_ENDPOINT) {
            url {
                parameters.append("method", "auth.getToken")
                parameters.append("api_key", apiKey)
                parameters.append("format", "json")
            }
        }.json("Last.fm")
        return response["token"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: throw ScrobbleProtocolException("Last.fm did not return an authorization token.")
    }

    fun authorizationUrl(token: String): String {
        ensureConfigured()
        require(token.isNotBlank()) { "Last.fm authorization token is empty." }
        return URLBuilder("https://www.last.fm/api/auth/").apply {
            parameters.append("api_key", apiKey)
            parameters.append("token", token)
        }.buildString()
    }

    suspend fun completeAuthorization(token: String): LastFmSession {
        ensureConfigured()
        require(token.isNotBlank()) { "Last.fm authorization token is empty." }
        val response = signedPost(linkedMapOf("method" to "auth.getSession", "token" to token)).json("Last.fm")
        val session = response["session"]?.jsonObject
            ?: throw ScrobbleProtocolException("Last.fm returned an incomplete account response.")
        val username = session["name"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: throw ScrobbleProtocolException("Last.fm returned an incomplete account response.")
        val key = session["key"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: throw ScrobbleProtocolException("Last.fm returned an incomplete account response.")
        return LastFmSession(username, key)
    }

    suspend fun updateNowPlaying(session: LastFmSession, track: ScrobbleTrackMetadata) {
        val values = track.parameters().toMutableMap()
        values["method"] = "track.updateNowPlaying"
        values["sk"] = session.sessionKey
        signedPost(values).json("Last.fm")
    }

    suspend fun scrobble(session: LastFmSession, track: ScrobbleTrackMetadata, timestampEpochSeconds: Long) {
        require(timestampEpochSeconds >= 0L) { "Listen timestamp must be non-negative." }
        val values = track.parameters().toMutableMap()
        values["method"] = "track.scrobble"
        values["sk"] = session.sessionKey
        values["timestamp"] = timestampEpochSeconds.toString()
        signedPost(values).json("Last.fm")
    }

    private suspend fun signedPost(values: Map<String, String>): HttpResponse {
        ensureConfigured()
        val parameters = values.toMutableMap().apply {
            this["api_key"] = apiKey
            this["format"] = "json"
            this["api_sig"] = lastFmApiSignature(this, sharedSecret)
        }
        return http.post(LAST_FM_ENDPOINT) {
            setBody(FormDataContent(Parameters.build {
                parameters.forEach { (key, value) -> append(key, value) }
            }))
        }
    }

    private fun ensureConfigured() {
        if (!configured) throw ScrobbleAuthenticationException("Last.fm linking is unavailable until Rynx API credentials are configured.")
    }

    private fun ScrobbleTrackMetadata.parameters(): MutableMap<String, String> {
        require(title.isNotBlank() && artist.isNotBlank()) { "Track title and artist are required for scrobbling." }
        return mutableMapOf("track" to title, "artist" to artist).apply {
            album?.takeIf(String::isNotBlank)?.let { put("album", it) }
            durationMillis?.takeIf { it > 0L }?.let { put("duration", (it / 1_000L).toString()) }
        }
    }

    private companion object { const val LAST_FM_ENDPOINT = "https://ws.audioscrobbler.com/2.0/" }
}

class ListenBrainzClient(
    private val http: HttpClient,
    private val userAgent: String = DEFAULT_USER_AGENT,
) {
    suspend fun validateToken(token: String): ListenBrainzAccount {
        require(token.isNotBlank()) { "ListenBrainz token is empty." }
        val response = http.get("$LISTENBRAINZ_API/1/validate-token") {
            header(HttpHeaders.Authorization, "Token $token")
            header(HttpHeaders.UserAgent, userAgent)
        }.json("ListenBrainz")
        if (response["valid"]?.jsonPrimitive?.contentOrNull != "true") throw ScrobbleAuthenticationException("ListenBrainz rejected this token.")
        val username = response["user_name"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank)
            ?: throw ScrobbleProtocolException("ListenBrainz returned no account name.")
        return ListenBrainzAccount(username)
    }

    suspend fun updateNowPlaying(token: String, track: ScrobbleTrackMetadata) = submit(token, track, null)

    suspend fun submitListen(token: String, track: ScrobbleTrackMetadata, listenedAtEpochSeconds: Long) {
        require(listenedAtEpochSeconds >= 0L) { "Listen timestamp must be non-negative." }
        submit(token, track, listenedAtEpochSeconds)
    }

    private suspend fun submit(token: String, track: ScrobbleTrackMetadata, listenedAt: Long?) {
        require(token.isNotBlank()) { "ListenBrainz token is empty." }
        require(track.title.isNotBlank() && track.artist.isNotBlank()) { "Track title and artist are required for scrobbling." }
        val metadata = buildJsonObject {
            put("track_name", track.title)
            put("artist_name", track.artist)
            track.album?.takeIf(String::isNotBlank)?.let { put("release_name", it) }
            put("additional_info", buildJsonObject {
                track.durationMillis?.takeIf { it > 0L }?.let { put("duration_ms", it) }
            })
        }
        val listen = buildJsonObject {
            listenedAt?.let { put("listened_at", it) }
            put("track_metadata", metadata)
        }
        val payload = buildJsonObject {
            put("listen_type", if (listenedAt == null) "playing_now" else "single")
            put("payload", buildJsonArray { add(listen) })
        }
        val response = http.post("$LISTENBRAINZ_API/1/submit-listens") {
            header(HttpHeaders.Authorization, "Token $token")
            header(HttpHeaders.UserAgent, userAgent)
            contentType(ContentType.Application.Json)
            setBody(payload.toString())
        }
        if (!response.status.isSuccess()) throw ScrobbleNetworkException("ListenBrainz request failed (HTTP ${response.status.value}).")
    }

    private companion object {
        const val LISTENBRAINZ_API = "https://api.listenbrainz.org"
        const val DEFAULT_USER_AGENT = "rynx/0.1.0 (https://github.com/bxanedot/rynx)"
    }
}

private suspend fun HttpResponse.json(provider: String): JsonObject {
    if (!status.isSuccess()) throw ScrobbleNetworkException("$provider request failed (HTTP ${status.value}).")
    val body = bodyAsText()
    val result = runCatching { Json.parseToJsonElement(body).jsonObject }
        .getOrElse { throw ScrobbleProtocolException("$provider returned an unreadable response.") }
    val providerError = result["error"]?.jsonPrimitive?.intOrNull
    if (providerError != null) {
        if (providerError == 4 || providerError == 6 || providerError == 9) {
            throw ScrobbleAuthenticationException("$provider rejected the account authorization (code $providerError).")
        }
        throw ScrobbleNetworkException("$provider request failed (code $providerError).")
    }
    return result
}
