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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Status layar dunia pastel — semuanya memakai maskot SelaBlob:
 * [SelaMascotExpression.Thinking] untuk memuat, [SelaMascotExpression.Sleeping]
 * untuk kosong. Tidak ada spinner kaku atau ikon suram.
 */

/** Status memuat — maskot Thinking di kartu kaca melayang. */
@Composable
fun PastelLoadingState(
    title: String = "Memuat data perjalanan",
    subtitle: String = "Halte, rute, dan posisi bus",
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SelaPastelShapes.lg,
        color = Color.White.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
        modifier = modifier
            .pastelShadow(elevation = SelaPastelShadows.card, shape = SelaPastelShapes.lg)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelaMascot(
                expression = SelaMascotExpression.Thinking,
                diameter = 64.dp,
                bodyColor = SelaPastel.lavender,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }
        }
    }
}

/** Status kosong — maskot Sleeping + pesan ramah. */
@Composable
fun PastelEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 96.dp,
    bodyColor: Color = SelaPastel.blue,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelaMascot(
            expression = SelaMascotExpression.Sleeping,
            diameter = mascotSize,
            bodyColor = bodyColor,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = SelaPastel.inkSoft,
            textAlign = TextAlign.Center,
        )
    }
}

/** Status galat — kartu danger pastel + tombol coba lagi. */
@Composable
fun PastelErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PastelCard(
        tint = SelaPastel.dangerSoft,
        shadowTint = SelaPastel.dangerDeep,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = null,
                tint = SelaPastel.dangerDeep,
                modifier = Modifier.size(28.dp),
            )
            Text(
                message,
                color = SelaPastel.ink,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            SelaButton(
                text = "Coba lagi",
                onClick = onRetry,
                style = SelaButtonStyle.Soft,
                accent = SelaPastel.danger,
                onAccent = SelaPastel.dangerDeep,
                icon = Icons.Default.Refresh,
            )
        }
    }
}

/** Nada spanduk info pastel. */
enum class PastelNoticeTone { NEUTRAL, LIVE, WARNING }

/** Spanduk info ringkas — pil pastel untuk status koneksi & mode offline. */
@Composable
fun PastelNoticeBanner(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.WifiOff,
    tone: PastelNoticeTone = PastelNoticeTone.NEUTRAL,
) {
    val (bg, fg) = when (tone) {
        PastelNoticeTone.NEUTRAL -> Color.White.copy(alpha = 0.85f) to SelaPastel.inkSoft
        PastelNoticeTone.LIVE -> SelaPastel.mintSoft to SelaPastel.mintDeep
        PastelNoticeTone.WARNING -> SelaPastel.warningSoft to SelaPastel.sunnyDeep
    }
    val iconFallback = if (icon == Icons.Default.WifiOff && tone == PastelNoticeTone.NEUTRAL) {
        Icons.Default.Info
    } else icon
    Surface(
        shape = SelaPastelShapes.pill,
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        modifier = modifier
            .pastelShadow(elevation = SelaPastelShadows.soft, shape = SelaPastelShapes.pill)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(iconFallback, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}
