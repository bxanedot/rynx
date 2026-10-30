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
    )

    fun refreshIfStale() {
        if (shouldRefreshDiscovery(current, lastAttemptMillis, System.currentTimeMillis())) load()
    }

    fun load(region: String = current.region) {
        lastAttemptMillis = System.currentTimeMillis()
        job?.cancel()
        val selected = region.takeIf { code -> Regions.all.any { it.code == code } } ?: "us"
        prefs.edit { putString("region", selected) }
        current = current.copy(enabled = true, loading = true, region = selected, error = null, sections = emptyList())
        onFeed(current)
        job = scope.launch {
            try {
                val shelves = listOf(
                    "trending music" to "Trending",
                    "new music releases" to "New Releases",
                    "popular music" to "Popular",
                )
                val sections = shelves.map { (query, title) ->
                    async(Dispatchers.IO) {
                        val tracks = runCatching { ytmSearch(query) }.getOrDefault(emptyList())
                        HomeSection(query, title, "YouTube Music", HomeSectionKind.Trending, tracks)
                    }
                }.awaitAll()
                ensureActive()
                current = current.copy(
                    loading = false,
                    sections = sections,
                    updatedMillis = System.currentTimeMillis(),
                    stale = false,
                    error = null,
                )
                onFeed(current)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                current = current.copy(loading = false, error = "YouTube Music could not load. Check your connection.")
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
