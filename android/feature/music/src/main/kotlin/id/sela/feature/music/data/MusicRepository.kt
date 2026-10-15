package id.sela.feature.music.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.SearchFilter
import moe.rukamori.archivetune.innertube.YouTube
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Akses katalog YouTube Music via InnerTube (tanpa login).
 * Seluruh panggilan jaringan di IO dispatcher; error dibungkus Result.
 */
@Singleton
class MusicRepository @Inject constructor() {

    suspend fun home(): Result<List<HomeSection>> = withContext(Dispatchers.IO) {
        YouTube.home().map { it.toHomeSections() }
    }

    suspend fun search(query: String): Result<MusicSearchResult> = withContext(Dispatchers.IO) {
        // Cari lagu dulu (paling relevan untuk playback), lalu artis/album/playlist paralel sederhana
        val songs = YouTube.search(query, SearchFilter.FILTER_SONG).getOrElse { return@withContext Result.failure(it) }
        val artists = YouTube.search(query, SearchFilter.FILTER_ARTIST).getOrNull()
        val albums = YouTube.search(query, SearchFilter.FILTER_ALBUM).getOrNull()
        val playlists = YouTube.search(query, SearchFilter.FILTER_FEATURED_PLAYLIST).getOrNull()
        val merged = (songs.items + (artists?.items.orEmpty()) + (albums?.items.orEmpty()) + (playlists?.items.orEmpty()))
        Result.success(merged.toMusicSearchResult())
    }

    suspend fun searchSuggestions(query: String): Result<List<String>> = withContext(Dispatchers.IO) {
        YouTube.searchSuggestions(query).map { page ->
            page.queries.take(8)
        }
    }

    suspend fun artist(browseId: String): Result<SelaArtist> = withContext(Dispatchers.IO) {
        YouTube.artist(browseId).map { it.toSelaArtist().copy(id = browseId) }
    }

    suspend fun album(browseId: String): Result<SelaAlbum> = withContext(Dispatchers.IO) {
        YouTube.album(browseId).map { it.toSelaAlbum().copy(id = browseId) }
    }

    suspend fun playlist(playlistId: String): Result<SelaPlaylist> = withContext(Dispatchers.IO) {
        YouTube.playlist(playlistId).map { it.toSelaPlaylist().copy(id = playlistId) }
    }

    /**
     * Ambil lagu-lagu terkait (autoplay / radio) untuk antrean otomatis.
     */
    suspend fun relatedSongs(videoId: String): Result<List<SelaSong>> = withContext(Dispatchers.IO) {
        runCatching {
            val next = YouTube.next(
                endpoint = moe.rukamori.archivetune.innertube.models.WatchEndpoint(videoId = videoId),
            ).getOrThrow()
            next.items.filterIsInstance<moe.rukamori.archivetune.innertube.models.SongItem>()
                .map { it.toSelaSong() }
        }
    }
}
