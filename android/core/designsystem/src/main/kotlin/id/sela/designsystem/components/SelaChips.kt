package id.sela.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Titik status berdenyut — untuk koneksi live & ETA.
 * Komponen #80 (Metric Indicator, versi live).
 */
@Composable
fun SelaPulseDot(
    active: Boolean,
    modifier: Modifier = Modifier.size(10.dp),
    color: Color = SelaPalette.volt,
) {
    if (active) {
        val pulse = rememberInfiniteTransition(label = "pulse")
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
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier,
        ) {}
    }
}

/**
 * Lencana rute — pil kecil berwarna layanan.
 * Komponen #64 (Badge).
 */
@Composable
fun SelaRouteBadge(
    label: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SelaTokens.pill,
        color = color.copy(alpha = 0.14f),
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
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
 * Chip filter — pil dengan titik warna layanan.
 * Komponen #65 (Chip).
 */
@Composable
fun SelaFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(180),
        label = "chipBg",
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "chipFg",
    )
    Surface(
        onClick = onClick,
        shape = SelaTokens.pill,
        color = background,
        contentColor = content,
        border = BorderStroke(
            1.dp,
            if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .heightIn(min = SelaTokens.touchTarget)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dotColor != null) {
                Surface(shape = CircleShape, color = dotColor, modifier = Modifier.size(8.dp)) {}
            }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/**
 * Kontrol segmen — pil berisi opsi, opsi aktif menonjol volt.
 * Komponen #9 (Segmented Control).
 */
@Composable
fun SelaSegmentedControl(
    options: List<SelaSegmentOption>,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = SelaTokens.pill,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = SelaTokens.floatElevation,
        modifier = modifier,
    ) {
        Row(
            Modifier
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEach { option ->
                val background by animateColorAsState(
                    targetValue = if (option.selected) SelaPalette.volt else Color.Transparent,
                    animationSpec = tween(180),
                    label = "segBg",
                )
                val content by animateColorAsState(
                    targetValue = if (option.selected) SelaPalette.onVolt else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180),
                    label = "segFg",
                )
                Surface(
                    onClick = option.onClick,
                    shape = SelaTokens.pill,
                    color = background,
                    contentColor = content,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .semantics { role = Role.Tab; selected = option.selected },
                ) {
                    Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (option.dotColor != null) {
                                Surface(shape = CircleShape, color = option.dotColor, modifier = Modifier.size(8.dp)) {}
                            }
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

data class SelaSegmentOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val dotColor: Color? = null,
)
