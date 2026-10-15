package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.data.SelaSong

@Composable
fun MusicArtwork(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    cornerDp: Int = 12,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerDp.dp))
            .background(SelaPastel.lavenderSoft),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = SelaPastel.lavenderDeep,
                modifier = Modifier.size(24.dp),
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(cornerDp.dp)),
            )
        }
    }
}

@Composable
fun SongRow(
    song: SelaSong,
    onClick: () -> Unit,
    onMore: (() -> Unit)? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MusicArtwork(
            url = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SelaPastel.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                song.artistNames,
                style = MaterialTheme.typography.bodySmall,
                color = SelaPastel.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        song.durationSec?.let { dur ->
            Text(
                formatDuration(dur * 1000L),
                style = MaterialTheme.typography.bodySmall,
                color = SelaPastel.inkFaint,
            )
            Spacer(Modifier.width(4.dp))
        }
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorit",
                    tint = if (isFavorite) SelaPastel.pinkDeep else SelaPastel.inkFaint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (onMore != null) {
            IconButton(onClick = onMore, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Opsi",
                    tint = SelaPastel.inkFaint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun MusicSectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SelaPastel.ink,
        )
        if (subtitle != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SelaPastel.inkSoft,
            )
        }
    }
}

@Composable
fun SongCard(
    song: SelaSong,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color = SelaPastel.surface,
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardColor)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        MusicArtwork(
            url = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            cornerDp = 14,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = SelaPastel.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            minLines = 2,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            song.artistNames,
            style = MaterialTheme.typography.labelSmall,
            color = SelaPastel.inkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

fun formatDuration(ms: Long): String {
    if (ms < 0) return "--:--"
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

@Composable
fun MusicEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    showMascot: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (showMascot) {
                // Maskot 2.5D Sela — ekspresi Empty (sleepy + calm float)
                id.sela.designsystem.SelaMascot(
                    expression = id.sela.designsystem.SelaMascotExpression.Empty,
                    diameter = 96.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SelaPastel.blueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = SelaPastel.blueDeep,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = SelaPastel.inkSoft,
            )
        }
    }
}

@Composable
fun MusicLoadingRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SelaPastel.creamDeep),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(SelaPastel.creamDeep),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SelaPastel.creamDeep),
            )
        }
    }
}
