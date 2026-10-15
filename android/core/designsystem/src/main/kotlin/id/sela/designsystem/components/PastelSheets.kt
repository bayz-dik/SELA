package id.sela.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Elemen sheet & daftar dunia pastel: bottom sheet putih lembut dengan
 * sudut 32dp, baris daftar berikon pastel, lencana rute, dan titik denyut.
 */

/** Gagang seret pastel — pil lavender pendek. */
@Composable
fun PastelDragHandle(modifier: Modifier = Modifier) {
    Surface(
        shape = SelaPastelShapes.pill,
        color = SelaPastel.lavender.copy(alpha = 0.85f),
        modifier = modifier
            .padding(vertical = 12.dp)
            .size(width = 48.dp, height = 6.dp),
    ) {}
}

/** Bottom sheet modal pastel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastelBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SelaPastel.cream,
        contentColor = SelaPastel.ink,
        shape = SelaPastelShapes.sheet,
        dragHandle = { PastelDragHandle() },
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** Kepala bottom sheet pastel. */
@Composable
fun PastelSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SelaPastel.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }
}

/** Konten bergulir untuk isi sheet. */
@Composable
fun PastelSheetBody(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

/** Judul seksi kecil pastel. */
@Composable
fun PastelSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = SelaPastel.inkSoft,
        modifier = modifier.semantics { heading() },
    )
}

/** Baris kunci-nilai pastel — untuk detail di sheet. */
@Composable
fun PastelKeyValueRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        Modifier.fillMaxWidth().then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            key,
            style = MaterialTheme.typography.bodyMedium,
            color = SelaPastel.inkSoft,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = SelaPastel.ink,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

/**
 * Lencana rute pastel — pil tint layanan + teks deep (lolos kontras).
 */
@Composable
fun PastelRouteBadge(
    label: String,
    tint: Color = SelaPastel.blue,
    deep: Color = SelaPastel.blueDeep,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SelaPastelShapes.pill,
        color = tint.copy(alpha = 0.35f),
        contentColor = deep,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.6f)),
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Baris ikon + teks + aksi pastel, untuk daftar di bottom sheet.
 */
@Composable
fun PastelListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = SelaPastel.blue,
    leadingDeep: Color = SelaPastel.blueDeep,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = SelaPastelShapes.sm,
        color = Color.Transparent,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingIcon != null) {
                Surface(
                    shape = CircleShape,
                    color = leadingTint.copy(alpha = 0.35f),
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = leadingDeep,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink, maxLines = 2)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SelaPastel.inkSoft,
                        maxLines = 2,
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = SelaPastel.inkFaint,
                )
            }
        }
    }
}

/**
 * Titik status berdenyut pastel — untuk koneksi live & ETA.
 */
@Composable
fun PastelPulseDot(
    active: Boolean,
    modifier: Modifier = Modifier.size(10.dp),
    color: Color = SelaPastel.mintDeep,
) {
    if (active) {
        val pulse = rememberInfiniteTransition(label = "pastel-pulse")
        val alpha by pulse.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )
        Surface(
            shape = CircleShape,
            color = color,
            modifier = modifier.alpha(alpha),
        ) {}
    } else {
        Surface(
            shape = CircleShape,
            color = SelaPastel.hairline,
            modifier = modifier,
        ) {}
    }
}
