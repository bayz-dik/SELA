package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.playback.MusicPlayerState

/**
 * Mini-player mengambang — selalu terlihat di atas bottom dock saat ada lagu.
 * Tap: buka Now Playing. Tombol: play/pause + next.
 */
@Composable
fun MiniPlayer(
    state: MusicPlayerState,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song = state.currentSong ?: return
    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SelaPastel.surface.copy(alpha = 0.96f))
            .clickable(onClick = onOpenNowPlaying),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MusicArtwork(
                url = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier.size(44.dp),
                cornerDp = 12,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = SelaPastel.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song.artistNames,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = SelaPastel.blueDeep,
                    strokeWidth = 3.dp,
                )
            } else {
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(40.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SelaPastel.blueSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Jeda" else "Putar",
                            tint = SelaPastel.blueDeep,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
            IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = SelaPastel.inkSoft,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            color = SelaPastel.blueDeep,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
        )
    }
}
