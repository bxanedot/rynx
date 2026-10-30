package dev.bxane.rynx.android

import android.content.Context
import androidx.core.content.edit
import dev.bxane.rynx.playback.discovery.*
import dev.bxane.rynx.ui.TrackItem
import kotlinx.coroutines.*
import java.util.Locale

/** Fetches YouTube Music home shelves via InnerTubeX search. */
internal class AndroidDiscoveryCoordinator(
    context: Context,
    private val scope: CoroutineScope,
    private val onFeed: (DiscoveryState) -> Unit,
    private val onSelection: (HomeSection?, Boolean) -> Unit,
    private val ytmSearch: suspend (String) -> List<MusicMetadata>,
) {
    private val prefs = context.getSharedPreferences("discovery", Context.MODE_PRIVATE)
    private var job: Job? = null
    private var selectionJob: Job? = null
    private var lastAttemptMillis: Long? = null
    private var current = DiscoveryState(
        enabled = true,
        region = prefs.getString("region", null)
            ?: Locale.getDefault().country.lowercase().takeIf { code -> Regions.all.any { it.code == code } }
            ?: "us",
        regions = Regions.all,
        // These are search shortcuts, not a claim to have a provider's genre taxonomy.
        browseGenres = listOf(
            MusicGenre("pop", "Pop"),
            MusicGenre("hip-hop", "Hip-Hop"),
            MusicGenre("rnb", "R&B"),
            MusicGenre("electronic", "Electronic"),
            MusicGenre("indie", "Indie"),
            MusicGenre("rock", "Rock"),
            MusicGenre("jazz", "Jazz"),
            MusicGenre("classical", "Classical"),
        ),
    )

    fun refreshIfStale() {
        if (shouldRefreshDiscovery(current, lastAttemptMillis, System.currentTimeMillis())) load()
    }

    fun load(region: String = current.region) {
        lastAttemptMillis = System.currentTimeMillis()
        job?.cancel()
        val selected = region.takeIf { code -> Regions.all.any { it.code == code } } ?: "us"
        prefs.edit { putString("region", selected) }
        // Keep actual previous results visible while refreshing rather than making Home blank.
        current = current.copy(enabled = true, loading = true, region = selected, error = null)
        onFeed(current)
        job = scope.launch {
            try {
                val shelves = listOf(
                    "new music" to "New & Noteworthy",
                    "popular songs" to "Popular Right Now",
                    "chill music" to "Chill & Unwind",
                    "fresh discoveries" to "Fresh Finds",
                )
                val sections = shelves.map { (query, title) ->
                    async(Dispatchers.IO) {
                        try {
                            val tracks = ytmSearch(query)
                            HomeSection(query, title, "YouTube Music search", HomeSectionKind.Trending, tracks,
                                unavailableReason=if(tracks.isEmpty()) "No tracks returned. Try Search or refresh." else null)
                        } catch(cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            HomeSection(query,title,"YouTube Music search",HomeSectionKind.Trending,
                                unavailableReason="Could not load this shelf. Check connection or retry.")
                        }
                    }
                }.awaitAll()
                ensureActive()
                val online = sections.any { it.tracks.isNotEmpty() }
                current = current.copy(
                    loading = false,
                    sections = if(online || current.sections.isEmpty()) sections else current.sections,
                    updatedMillis = if(online) System.currentTimeMillis() else current.updatedMillis,
                    stale = !online,
                    error = if(online) null else "YouTube Music did not return playable search results. Check your connection, then retry.",
                )
                onFeed(current)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                current = current.copy(loading = false, stale = true, error = "YouTube Music could not load. Check your connection and retry.")
                onFeed(current)
            }
        }
    }

    fun section(id: String) {
        selectionJob?.cancel()
        onSelection(
            if (id == "regions") HomeSection("regions", "Chart region", "Choose a region", HomeSectionKind.Trending)
            else current.sections.firstOrNull { it.id == id },
            false,
        )
    }

    fun collection(value: ChartCollection) {
        selectionJob?.cancel()
        onSelection(HomeSection(value.id, value.title, "YouTube Music", HomeSectionKind.Trending, value.tracks, value.sourceUrl), false)
    }

    fun genre(value: MusicGenre) {
        selectionJob?.cancel()
        onSelection(HomeSection("genre:${value.id}", value.name, "YouTube Music", HomeSectionKind.Trending), true)
        selectionJob = scope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) { ytmSearch("${value.name} music") }
                ensureActive()
                onSelection(HomeSection("genre:${value.id}", value.name, "YouTube Music", HomeSectionKind.Trending, tracks), false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                onSelection(HomeSection("genre:${value.id}", value.name, "YouTube Music", HomeSectionKind.Trending,
                    unavailableReason = "This genre could not load. Search remains available."), false)
            }
        }
    }

    fun closeSelection() { selectionJob?.cancel(); onSelection(null, false) }

    fun close() { job?.cancel(); selectionJob?.cancel() }
}
