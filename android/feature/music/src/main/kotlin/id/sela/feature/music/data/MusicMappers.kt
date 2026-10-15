package id.sela.feature.music.data

import moe.rukamori.archivetune.innertube.models.AlbumItem
import moe.rukamori.archivetune.innertube.models.ArtistItem
import moe.rukamori.archivetune.innertube.models.PlaylistItem
import moe.rukamori.archivetune.innertube.models.SongItem
import moe.rukamori.archivetune.innertube.models.YTItem
import moe.rukamori.archivetune.innertube.pages.AlbumPage
import moe.rukamori.archivetune.innertube.pages.ArtistPage
import moe.rukamori.archivetune.innertube.pages.HomePage
import moe.rukamori.archivetune.innertube.pages.PlaylistPage

internal fun SongItem.toSelaSong(): SelaSong = SelaSong(
    id = id,
    title = title,
    artistNames = artists.joinToString(", ") { it.name },
    artists = artists.map { SelaArtistRef(it.id, it.name) },
    albumName = album?.name,
    albumId = album?.id,
    durationSec = duration,
    thumbnailUrl = thumbnail,
    explicit = explicit,
)

internal fun ArtistItem.toSelaArtist(): SelaArtist = SelaArtist(
    id = id,
    name = title,
    thumbnailUrl = thumbnail,
    subscriberCount = subscriberCountText,
)

internal fun AlbumItem.toSelaAlbum(): SelaAlbum = SelaAlbum(
    id = id,
    title = title,
    artistNames = artists?.joinToString(", ") { it.name }.orEmpty(),
    year = year?.toString(),
    thumbnailUrl = thumbnail,
)

internal fun PlaylistItem.toSelaPlaylist(): SelaPlaylist = SelaPlaylist(
    id = id,
    title = title,
    author = author?.name,
    songCount = songCountText?.filter { it.isDigit() }?.toIntOrNull(),
    thumbnailUrl = thumbnail,
)

internal fun List<YTItem>.toMusicSearchResult(): MusicSearchResult = MusicSearchResult(
    songs = filterIsInstance<SongItem>().map { it.toSelaSong() },
    artists = filterIsInstance<ArtistItem>().map { it.toSelaArtist() },
    albums = filterIsInstance<AlbumItem>().map { it.toSelaAlbum() },
    playlists = filterIsInstance<PlaylistItem>().map { it.toSelaPlaylist() },
)

internal fun ArtistPage.toSelaArtist(): SelaArtist = SelaArtist(
    id = artist.id,
    name = artist.title,
    thumbnailUrl = artist.thumbnail,
    subscriberCount = artist.subscriberCountText,
)

internal fun AlbumPage.toSelaAlbum(): SelaAlbum = SelaAlbum(
    id = album.id,
    title = album.title,
    artistNames = album.artists?.joinToString(", ") { it.name }.orEmpty(),
    year = album.year?.toString(),
    thumbnailUrl = album.thumbnail,
    songs = songs.map { it.toSelaSong() },
)

internal fun PlaylistPage.toSelaPlaylist(): SelaPlaylist = SelaPlaylist(
    id = playlist.id,
    title = playlist.title,
    author = playlist.author?.name,
    songCount = songs.size,
    thumbnailUrl = playlist.thumbnail,
    songs = songs.map { it.toSelaSong() },
)

internal fun HomePage.toHomeSections(): List<HomeSection> = sections.mapNotNull { section ->
    val songs = section.items.filterIsInstance<SongItem>().map { it.toSelaSong() }
    if (songs.isEmpty()) null else HomeSection(section.title, songs)
}
