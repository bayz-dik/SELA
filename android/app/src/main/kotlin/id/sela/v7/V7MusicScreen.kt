package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.sela.designsystem.SelaV7
import id.sela.designsystem.components.V7Mascot
import id.sela.feature.music.ui.MusicViewModel

/**
 * SELA Music V7 — sesuai 08_MUSIC_ACUAN.png.
 * Top bar "SELA Music" + search, chip kategori (Untuk Kamu/Playlist/Mood),
 * banner maskot headphone, playlist populer, baru diputar.
 *
 * Playback tetap via MusicViewModel/Media3 yang sudah ada.
 */
@Composable
fun V7MusicScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (String) -> Unit = {},
) {
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val recentPlays by viewModel.recentPlays.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadHome() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "SELA Music",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = SelaV7.ink,
                )
                IconButton(onClick = { /* search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Cari", tint = SelaV7.ink)
                }
            }
        }

        // Chip kategori
        item {
            var selected by remember { mutableIntStateOf(0) }
            val chips = listOf("Untuk Kamu", "Playlist", "Mood")
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEachIndexed { i, label ->
                    FilterChip(
                        selected = selected == i,
                        onClick = { selected = i },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SelaV7.ink,
                            selectedLabelColor = Color.White,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Banner maskot headphone
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp),
                color = SelaV7.periSoft,
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Teman\nPerjalanan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = SelaV7.ink,
                        )
                        Surface(
                            shape = CircleShape,
                            color = SelaV7.surface,
                            modifier = Modifier.clickable { /* play */ },
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Putar",
                                tint = SelaV7.ink,
                                modifier = Modifier.padding(12.dp).size(28.dp),
                            )
                        }
                    }
                    V7Mascot(size = 120.dp, withHeadphones = true)
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Playlist populer
        item {
            Text(
                text = "Playlist Populer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SelaV7.ink,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            if (homeState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SelaV7.limeDeep)
                }
            } else if (homeState.sections.isEmpty()) {
                Text(
                    text = "Belum ada playlist. Coba lagi nanti.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.inkFaint,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(homeState.sections.take(6)) { section ->
                        V7PlaylistCard(
                            title = section.title,
                            onClick = { /* buka playlist */ },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Baru diputar
        item {
            Text(
                text = "Baru Diputar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SelaV7.ink,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        if (recentPlays.isEmpty()) {
            item {
                Text(
                    text = "Belum ada riwayat putar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.inkFaint,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(recentPlays.take(5)) { song ->
                V7SongRow(
                    title = song.title,
                    artist = song.artistNames,
                    onClick = { onSongClick(song.id) },
                )
            }
        }
    }
}

@Composable
private fun V7PlaylistCard(title: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SelaV7.limeSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = SelaV7.limeDeep,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = SelaV7.ink,
            maxLines = 1,
        )
    }
}

@Composable
private fun V7SongRow(title: String, artist: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SelaV7.periSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SelaV7.periDeep)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaV7.ink,
                maxLines = 1,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = SelaV7.inkSoft,
                maxLines = 1,
            )
        }
        IconButton(onClick = { /* menu */ }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Opsi", tint = SelaV7.inkFaint)
        }
    }
}
