package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Status memuat — kartu melayang ringkas.
 * Komponen #91 (Circular Progress) + #92 (Loading Spinner).
 */
@Composable
fun SelaLoadingState(
    title: String = "Memuat data perjalanan",
    subtitle: String = "Halte, rute, dan posisi bus",
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SelaTokens.card,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = SelaTokens.floatElevation,
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = SelaPalette.volt)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Status kosong — ikon + pesan, tanpa drama.
 * Komponen #95 (Empty State).
 */
@Composable
fun SelaEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.SearchOff,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.outline)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Status galat — ringkas, dengan aksi coba lagi.
 * Komponen #96 (Error State).
 */
@Composable
fun SelaErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SelaTokens.cardSmall,
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f)),
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            SelaGhostButton(text = "Coba lagi", onClick = onRetry)
        }
    }
}

/**
 * Spanduk info ringkas — untuk status koneksi & mode offline.
 * Komponen #88 (Banner Alert) + #98 (Offline State).
 */
@Composable
fun SelaNoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.WifiOff,
    tone: SelaNoticeTone = SelaNoticeTone.NEUTRAL,
) {
    val (bg, fg) = when (tone) {
        SelaNoticeTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        SelaNoticeTone.LIVE -> SelaPalette.voltDim to SelaPalette.volt
        SelaNoticeTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = SelaTokens.pill,
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = SelaTokens.floatElevation,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

enum class SelaNoticeTone { NEUTRAL, LIVE, WARNING }

/** Aksi coba-lagi generik berbentuk ikon. */
@Composable
fun SelaRetryIconButton(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    SelaRoundIconButton(icon = Icons.Default.Refresh, label = "Coba lagi", onClick = onRetry, modifier = modifier)
}
