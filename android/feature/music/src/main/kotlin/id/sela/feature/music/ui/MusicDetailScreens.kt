package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.data.MusicPlaylistEntity
import id.sela.feature.music.data.SelaAlbum
import id.sela.feature.music.data.SelaArtist
import id.sela.feature.music.data.SelaPlaylist
import id.sela.feature.music.data.SelaSong

@Composable
fun ArtistDetailScreen(
    artistId: String,
    artistName: String,
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var artist by remember { mutableStateOf<SelaArtist?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(artistId) {
        loading = true
        artist = viewModel.loadArtist(artistId).getOrNull()
        loading = false
    }
    DetailScaffold(
        title = artist?.name ?: artistName,
        subtitle = artist?.subscriberCount,
        artworkUrl = artist?.thumbnailUrl,
        isLoading = loading,
        onBack = onBack,
        modifier = modifier,
    ) {
        // Lagu-lagu artis: pakai pencarian nama artis sebagai fallback ringan
        item { MusicEmptyState("Ketuk lagu dari hasil pencarian untuk memutar.") }
    }
}

@Composable
fun AlbumDetailScreen(
    albumId: String,
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onPlayAll: (List<SelaSong>) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var album by remember { mutableStateOf<SelaAlbum?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(albumId) {
        loading = true
        album = viewModel.loadAlbum(albumId).getOrNull()
        loading = false
    }
    val songs = album?.songs.orEmpty()
    DetailScaffold(
        title = album?.title ?: "Album",
        subtitle = album?.let { "${it.artistNames} • ${it.year ?: ""}".trim() },
        artworkUrl = album?.thumbnailUrl,
        isLoading = loading,
        headerAction = if (songs.isNotEmpty()) ({
            PlayAllButton(onClick = { onPlayAll(songs) })
        }) else null,
        onBack = onBack,
        modifier = modifier,
    ) {
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                onClick = { onSongClick(song) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onPlayAll: (List<SelaSong>) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var playlist by remember { mutableStateOf<SelaPlaylist?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(playlistId) {
        loading = true
        playlist = viewModel.loadPlaylistDetail(playlistId).getOrNull()
        loading = false
    }
    val songs = playlist?.songs.orEmpty()
    DetailScaffold(
        title = playlist?.title ?: "Playlist",
        subtitle = playlist?.let {
            listOfNotNull(it.author, it.songCount?.let { n -> "$n lagu" }).joinToString(" • ")
        },
        artworkUrl = playlist?.thumbnailUrl,
        isLoading = loading,
        headerAction = if (songs.isNotEmpty()) ({
            PlayAllButton(onClick = { onPlayAll(songs) })
        }) else null,
        onBack = onBack,
        modifier = modifier,
    ) {
        items(songs, key = { it.id }) { song ->
            SongRow(
                song = song,
                onClick = { onSongClick(song) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

@Composable
fun LocalPlaylistDetailScreen(
    playlist: MusicPlaylistEntity,
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onPlayAll: (List<SelaSong>) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val songsFlow = remember(playlist.playlistId) { viewModel.playlistSongs(playlist.playlistId) }
    val songs by songsFlow.collectAsState()
    DetailScaffold(
        title = playlist.name,
        subtitle = "${songs.size} lagu",
        artworkUrl = songs.firstOrNull()?.thumbnailUrl,
        isLoading = false,
        headerAction = if (songs.isNotEmpty()) ({
            PlayAllButton(onClick = { onPlayAll(songs) })
        }) else null,
        onBack = onBack,
        modifier = modifier,
    ) {
        if (songs.isEmpty()) {
            item { MusicEmptyState("Playlist kosong. Tambah lagu dari hasil pencarian.") }
        } else {
            items(songs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayAllButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SelaPastel.blueDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("Putar semua", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailScaffold(
    title: String,
    subtitle: String?,
    artworkUrl: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
    headerAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SelaPastel.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = SelaPastel.ink)
            }
        }
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SelaPastel.blueDeep)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MusicArtwork(
                            url = artworkUrl,
                            contentDescription = title,
                            modifier = Modifier.size(96.dp),
                            cornerDp = 20,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = SelaPastel.ink,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            subtitle?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.inkSoft)
                            }
                            headerAction?.let {
                                Spacer(Modifier.height(12.dp))
                                // Bungkus agar bisa diklik
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier.clip(RoundedCornerShape(20.dp)),
                                ) { it() }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                content()
            }
        }
    }
}
