package dev.bxane.rynx.android

import dev.bxane.rynx.ui.TrackItem
import org.json.JSONArray
import org.json.JSONObject

/** Stores enough display metadata for a favorited remote track to survive app restarts. */
internal object TrackMetadataStorage {
    fun decode(value: String?): List<TrackItem> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").takeIf(String::isNotBlank) ?: continue
                    val title = item.optString("title").takeIf(String::isNotBlank) ?: continue
                    val subtitle = item.optString("subtitle").takeIf(String::isNotBlank) ?: "Unknown artist"
                    val artworkUrl = item.opt("artworkUrl")
                        ?.takeUnless { it == JSONObject.NULL }
                        ?.toString()
                        ?.takeIf(String::isNotBlank)
                    add(TrackItem(
                        id = id,
                        title = title,
                        subtitle = subtitle,
                        artworkUrl = artworkUrl,
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encode(tracks: List<TrackItem>): String = JSONArray().apply {
        tracks.distinctBy(TrackItem::id).forEach { track ->
            put(JSONObject()
                .put("id", track.id)
                .put("title", track.title)
                .put("subtitle", track.subtitle)
                .put("artworkUrl", track.artworkUrl ?: JSONObject.NULL))
        }
    }.toString()
}
