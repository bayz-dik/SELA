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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.data.SelaAlbum
import id.sela.feature.music.data.SelaArtist
import id.sela.feature.music.data.SelaPlaylist
import id.sela.feature.music.data.SelaSong

@Composable
fun MusicSearchScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onArtistClick: (SelaArtist) -> Unit = {},
    onAlbumClick: (SelaAlbum) -> Unit = {},
    onPlaylistClick: (SelaPlaylist) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val searchState by viewModel.searchState.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SelaPastel.background),
    ) {
        // Search bar
        OutlinedTextField(
            value = searchState.query,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            placeholder = { Text("Cari lagu, artis, album...", color = SelaPastel.inkFaint) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SelaPastel.inkSoft)
            },
            trailingIcon = {
                if (searchState.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = SelaPastel.inkFaint)
                    }
                }
            },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SelaPastel.blueDeep,
                unfocusedBorderColor = SelaPastel.hairline,
                focusedContainerColor = SelaPastel.surface,
                unfocusedContainerColor = SelaPastel.surface,
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search(searchState.query) }),
        )

        when {
            searchState.isLoading -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(6) { MusicLoadingRow() }
                }
            }
            searchState.query.isBlank() -> {
                // Riwayat pencarian
                if (searchHistory.isNotEmpty()) {
                    Text(
                        "Terakhir dicari",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SelaPastel.ink,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
                        items(searchHistory) { q ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.search(q) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = SelaPastel.inkFaint,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    q,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SelaPastel.ink,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { viewModel.removeSearchHistory(q) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = SelaPastel.inkFaint,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    MusicEmptyState("Ketik untuk mencari musik favoritmu.")
                }
            }
            searchState.suggestions.isNotEmpty() &&
                searchState.songs.isEmpty() &&
                searchState.artists.isEmpty() -> {
                // Saran pencarian (sebelum submit)
                LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
                    items(searchState.suggestions) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.search(s) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = SelaPastel.inkFaint,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(s, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.ink)
                        }
                    }
                }
            }
            else -> {
                // Hasil pencarian
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    if (searchState.songs.isNotEmpty()) {
                        item {
                            MusicSectionTitle(
                                "Lagu",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(searchState.songs, key = { it.id }) { song ->
                            SongRow(
                                song = song,
                                onClick = { onSongClick(song) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                    if (searchState.artists.isNotEmpty()) {
                        item {
                            MusicSectionTitle(
                                "Artis",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(searchState.artists, key = { it.id }) { artist ->
                            ArtistRow(artist = artist, onClick = { onArtistClick(artist) })
                        }
                    }
                    if (searchState.albums.isNotEmpty()) {
                        item {
                            MusicSectionTitle(
                                "Album",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(searchState.albums, key = { it.id }) { album ->
                            AlbumRow(album = album, onClick = { onAlbumClick(album) })
                        }
                    }
                    if (searchState.playlists.isNotEmpty()) {
                        item {
                            MusicSectionTitle(
                                "Playlist",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        items(searchState.playlists, key = { it.id }) { playlist ->
                            PlaylistRow(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                        }
                    }
                    if (searchState.songs.isEmpty() && searchState.artists.isEmpty() &&
                        searchState.albums.isEmpty() && searchState.playlists.isEmpty()
                    ) {
                        item { MusicEmptyState("Tidak ketemu. Coba kata kunci lain.") }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistRow(artist: SelaArtist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(SelaPastel.mintSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (artist.thumbnailUrl.isNullOrBlank()) {
                Text(
                    artist.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SelaPastel.mintDeep,
                )
            } else {
                MusicArtwork(
                    url = artist.thumbnailUrl,
                    contentDescription = artist.name,
                    modifier = Modifier.size(52.dp),
                    cornerDp = 26,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                artist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaPastel.ink,
            )
            artist.subscriberCount?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = SelaPastel.inkSoft)
            }
        }
    }
}

@Composable
fun AlbumRow(album: SelaAlbum, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArtwork(url = album.thumbnailUrl, contentDescription = album.title, modifier = Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                album.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaPastel.ink,
            )
            val sub = listOfNotNull(album.artistNames.takeIf { it.isNotBlank() }, album.year)
                .joinToString(" • ")
            Text(sub, style = MaterialTheme.typography.bodySmall, color = SelaPastel.inkSoft)
        }
    }
}

@Composable
fun PlaylistRow(playlist: SelaPlaylist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArtwork(url = playlist.thumbnailUrl, contentDescription = playlist.title, modifier = Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaPastel.ink,
            )
            val sub = listOfNotNull(
                playlist.author,
                playlist.songCount?.let { "$it lagu" },
            ).joinToString(" • ")
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = SelaPastel.inkSoft)
            }
        }
    }
}
