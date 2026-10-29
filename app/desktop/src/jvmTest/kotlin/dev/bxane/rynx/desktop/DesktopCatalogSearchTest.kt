package dev.bxane.rynx.desktop

import dev.bxane.rynx.playback.catalog.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import java.nio.file.Files
import kotlin.test.*

class DesktopCatalogSearchTest {
    private class FakeCatalog : CatalogSearchRepository {
        val queries = mutableListOf<String>()
        val continuations = mutableListOf<String>()
        var shouldFail = false

        override suspend fun searchMusic(query: String): CatalogPage {
            queries += query
            if(shouldFail) error("transport URL https://example.invalid/?token=secret")
            return CatalogPage(listOf(item("one", "First Song")), "page-2")
        }

        override suspend fun searchMore(continuation: String): CatalogPage {
            continuations += continuation
            return CatalogPage(listOf(item("one", "First Song"),item("two", "Second Song")),null)
        }
    }

    @Test fun searchTrimsQueryAndPaginationDeduplicatesProviderItems() = runBlocking {
        val dir=Files.createTempDirectory("rynx-catalog-").toFile()
        val catalog=FakeCatalog()
        val player=DesktopPlayer(LocalLibraryStore(dir.resolve("library.txt")),catalogSearch=catalog)
        try {
            player.searchYouTubeMusic("  ambient jazz  ")
            val first=withTimeout(5000){player.state.first{it.catalogSearch.items.size==1&&!it.catalogSearch.loading}}
            assertEquals("ambient jazz",first.catalogSearch.query)
            assertEquals(listOf("ambient jazz"),catalog.queries)
            assertEquals("page-2",first.catalogSearch.continuation)

            player.loadMoreYouTubeMusic()
            val second=withTimeout(5000){player.state.first{!it.catalogSearch.loadingMore&&it.catalogSearch.continuation==null}}
            assertEquals(listOf("page-2"),catalog.continuations)
            assertEquals(listOf("one","two"),second.catalogSearch.items.map{it.mediaId})
        } finally {player.close();dir.deleteRecursively()}
    }

    @Test fun providerErrorsDoNotExposeTransportDetails() = runBlocking {
        val dir=Files.createTempDirectory("rynx-catalog-error-").toFile()
        val catalog=FakeCatalog().apply{shouldFail=true}
        val player=DesktopPlayer(LocalLibraryStore(dir.resolve("library.txt")),catalogSearch=catalog)
        try {
            player.searchYouTubeMusic("ambient")
            val failed=withTimeout(5000){player.state.first{it.catalogSearch.error!=null}}
            assertEquals("YouTube Music search failed. Check your connection and try again.",failed.catalogSearch.error)
            assertFalse(failed.catalogSearch.error!!.contains("secret"))
            assertFalse(failed.catalogSearch.error!!.contains("https://"))
        } finally {player.close();dir.deleteRecursively()}
    }

}

private fun item(id:String,title:String)=CatalogItem(CatalogProvider.YouTubeMusic,CatalogItemType.Track,id,title,"Artist")
