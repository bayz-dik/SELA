package id.sela.feature.music.data

/**
 * Model musik SELA — bersih dari detail InnerTube.
 * Dipetakan dari moe.rukamori.archivetune.innertube.models.YTItem.
 */
data class SelaSong(
    val id: String, // videoId YouTube Music
    val title: String,
    val artistNames: String,
    val artists: List<SelaArtistRef> = emptyList(),
    val albumName: String? = null,
    val albumId: String? = null,
    val durationSec: Int? = null,
    val thumbnailUrl: String? = null,
    val explicit: Boolean = false,
)

data class SelaArtistRef(val id: String?, val name: String)

data class SelaArtist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscriberCount: String? = null,
)

data class SelaAlbum(
    val id: String,
    val title: String,
    val artistNames: String,
    val year: String? = null,
    val thumbnailUrl: String? = null,
    val songs: List<SelaSong> = emptyList(),
)

data class SelaPlaylist(
    val id: String,
    val title: String,
    val author: String? = null,
    val songCount: Int? = null,
    val thumbnailUrl: String? = null,
    val songs: List<SelaSong> = emptyList(),
)

data class MusicSearchResult(
    val songs: List<SelaSong> = emptyList(),
    val artists: List<SelaArtist> = emptyList(),
    val albums: List<SelaAlbum> = emptyList(),
    val playlists: List<SelaPlaylist> = emptyList(),
)

data class HomeSection(
    val title: String,
    val songs: List<SelaSong>,
)

/** Hasil resolve stream untuk satu lagu. */
data class ResolvedStream(
    val songId: String,
    val streamUrl: String,
    val expiresAtMs: Long,
    val mimeType: String,
)

fun ResolvedStream.isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
    nowMs + 60_000L >= expiresAtMs
