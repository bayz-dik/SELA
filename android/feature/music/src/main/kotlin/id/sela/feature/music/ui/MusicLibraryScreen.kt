package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.data.MusicPlaylistEntity
import id.sela.feature.music.data.SelaSong

@Composable
fun MusicLibraryScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onPlaylistClick: (MusicPlaylistEntity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val recentPlays by viewModel.recentPlays.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SelaPastel.background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item {
            Text(
                "Pustaka",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SelaPastel.ink,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
        // Favorit
        item {
            LibrarySectionHeader(
                icon = Icons.Default.Favorite,
                title = "Favorit",
                subtitle = "${favorites.size} lagu",
                tint = SelaPastel.pinkSoft,
                iconTint = SelaPastel.pinkDeep,
            )
        }
        if (favorites.isEmpty()) {
            item { MusicEmptyState("Belum ada lagu favorit. Ketuk ikon hati untuk menyimpan.") }
        } else {
            items(favorites.take(5), key = { it.id }) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(song) },
                    isFavorite = true,
                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
        // Playlist
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibrarySectionHeader(
                    icon = Icons.Default.PlaylistPlay,
                    title = "Playlist saya",
                    subtitle = "${playlists.size} playlist",
                    tint = SelaPastel.blueSoft,
                    iconTint = SelaPastel.blueDeep,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Buat playlist", tint = SelaPastel.blueDeep)
                }
            }
        }
        if (playlists.isEmpty()) {
            item { MusicEmptyState("Belum ada playlist. Buat satu untuk menyimpan lagu.") }
        } else {
            items(playlists, key = { it.playlistId }) { playlist ->
                PlaylistItemRow(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) },
                    onDelete = { viewModel.deletePlaylist(playlist.playlistId) },
                )
            }
        }
        // Terakhir diputar
        item {
            LibrarySectionHeader(
                icon = Icons.Default.History,
                title = "Terakhir diputar",
                subtitle = null,
                tint = SelaPastel.mintSoft,
                iconTint = SelaPastel.mintDeep,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        if (recentPlays.isEmpty()) {
            item { MusicEmptyState("Riwayat putar masih kosong.") }
        } else {
            items(recentPlays.take(10), key = { it.id + it.title }) { song ->
                SongRow(
                    song = song,
                    onClick = { onSongClick(song) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Playlist baru", color = SelaPastel.ink) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Nama playlist", color = SelaPastel.inkFaint) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SelaPastel.blueDeep,
                        unfocusedBorderColor = SelaPastel.hairline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                ) { Text("Buat", color = SelaPastel.blueDeep) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Batal", color = SelaPastel.inkSoft)
                }
            },
            containerColor = SelaPastel.surface,
        )
    }
}

@Composable
private fun LibrarySectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    tint: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SelaPastel.ink,
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = SelaPastel.inkSoft)
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    playlist: MusicPlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SelaPastel.lavenderSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = SelaPastel.lavenderDeep,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaPastel.ink,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Hapus",
                tint = SelaPastel.inkFaint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
