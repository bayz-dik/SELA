package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.playback.MusicPlayerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: MusicPlayerState,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeekToIndex: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SelaPastel.lavenderSoft, SelaPastel.background, SelaPastel.background),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Tutup",
                        tint = SelaPastel.ink,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "SEDANG DIPUTAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = SelaPastel.inkFaint,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))

            // Artwork besar
            MusicArtwork(
                url = song?.thumbnailUrl,
                contentDescription = song?.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                cornerDp = 28,
            )
            Spacer(Modifier.height(24.dp))

            // Judul + artis + favorit
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        song?.title ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SelaPastel.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        song?.artistNames ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorit",
                        tint = if (isFavorite) SelaPastel.pinkDeep else SelaPastel.inkFaint,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Progress bar
            val progress = if (state.durationMs > 0) {
                state.positionMs.toFloat() / state.durationMs.toFloat()
            } else 0f
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { v -> onSeekTo((v * state.durationMs).toLong()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = SelaPastel.blueDeep,
                    activeTrackColor = SelaPastel.blueDeep,
                    inactiveTrackColor = SelaPastel.blueSoft,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    formatDuration(state.positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkFaint,
                )
                Text(
                    formatDuration(state.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkFaint,
                )
            }
            Spacer(Modifier.height(8.dp))

            // Kontrol utama
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Acak",
                        tint = if (state.shuffleOn) SelaPastel.blueDeep else SelaPastel.inkFaint,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = SelaPastel.ink,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SelaPastel.blueDeep),
                ) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = SelaPastel.ink,
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = onCycleRepeat, modifier = Modifier.size(48.dp)) {
                    Icon(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                        else Icons.Default.Repeat,
                        contentDescription = "Ulangi",
                        tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) SelaPastel.blueDeep
                        else SelaPastel.inkFaint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Tombol antrean
            if (state.queue.isNotEmpty()) {
                androidx.compose.material3.TextButton(onClick = { showQueue = true }) {
                    Text(
                        "Antrean (${state.queue.size} lagu)",
                        color = SelaPastel.blueDeep,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            state.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(
                    err,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.pinkDeep,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = SelaPastel.background,
        ) {
            Text(
                "Antrean",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SelaPastel.ink,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
                itemsIndexed(state.queue) { index, qSong ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                            if (index == state.queueIndex) {
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SelaPastel.blueDeep),
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SelaPastel.inkFaint,
                                )
                            }
                        }
                        SongRow(
                            song = qSong,
                            onClick = {
                                onSeekToIndex(index)
                                scope.launch { sheetState.hide(); showQueue = false }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
