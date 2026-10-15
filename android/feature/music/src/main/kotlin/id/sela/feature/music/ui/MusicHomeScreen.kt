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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.data.HomeSection
import id.sela.feature.music.data.SelaSong

@Composable
fun MusicHomeScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val homeState by viewModel.homeState.collectAsState()
    val playerState by viewModel.controller.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SelaPastel.blueSoft.copy(alpha = 0.5f), SelaPastel.background),
                ),
            ),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Musik",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = SelaPastel.ink,
                )
                Text(
                    "Teman perjalananmu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaPastel.inkSoft,
                )
            }
            IconButton(onClick = { viewModel.loadHome() }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Muat ulang",
                    tint = SelaPastel.inkSoft,
                )
            }
        }

        when {
            homeState.isLoading -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(8) { MusicLoadingRow() }
                }
            }
            homeState.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            homeState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SelaPastel.inkSoft,
                        )
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.material3.TextButton(onClick = { viewModel.loadHome() }) {
                            Text("Coba lagi", color = SelaPastel.blueDeep)
                        }
                    }
                }
            }
            homeState.sections.isEmpty() -> {
                MusicEmptyState("Belum ada rekomendasi. Coba cari lagu dulu.")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Hero: lagu pertama dari section pertama
                    homeState.sections.firstOrNull()?.songs?.firstOrNull()?.let { featured ->
                        item {
                            FeaturedSongHero(
                                song = featured,
                                isPlaying = playerState.currentSong?.id == featured.id && playerState.isPlaying,
                                onPlay = { onSongClick(featured) },
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    items(homeState.sections) { section ->
                        HomeSectionRow(
                            section = section,
                            onSongClick = onSongClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedSongHero(
    song: SelaSong,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(SelaPastel.lavenderSoft, SelaPastel.blueSoft),
                ),
            )
            .clickable(onClick = onPlay)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArtwork(
            url = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier.size(88.dp),
            cornerDp = 18,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "PILIHAN HARI INI",
                style = MaterialTheme.typography.labelSmall,
                color = SelaPastel.lavenderDeep,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SelaPastel.ink,
                maxLines = 2,
            )
            Text(
                song.artistNames,
                style = MaterialTheme.typography.bodySmall,
                color = SelaPastel.inkSoft,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(SelaPastel.blueDeep)
                .clickable(onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                contentDescription = "Putar",
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun HomeSectionRow(
    section: HomeSection,
    onSongClick: (SelaSong) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        MusicSectionTitle(
            title = section.title,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(section.songs, key = { it.id }) { song ->
                val cardColors = listOf(
                    SelaPastel.surface, SelaPastel.blueSoft.copy(alpha = 0.4f),
                    SelaPastel.mintSoft.copy(alpha = 0.4f), SelaPastel.lavenderSoft.copy(alpha = 0.4f),
                )
                SongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                    cardColor = cardColors[song.id.hashCode().mod(cardColors.size).let { if (it < 0) it + cardColors.size else it }],
                )
            }
        }
    }
}
