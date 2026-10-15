package id.sela.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Gaya tombol pastel. */
enum class SelaButtonStyle {
    /** Gradien aurora + teks gelap — aksi utama. */
    Primary,

    /** Tint pastel lembut + teks deep — aksi pendukung. */
    Soft,
}

/**
 * Tombol pastel — pill membulat dengan efek tekan: mengecil 0.94x
 * memakai spring memantul, lalu kembali. Playful tapi tetap responsif.
 *
 * @param accent warna aksen untuk gaya [SelaButtonStyle.Soft]
 *        (latar tint + teks deep).
 */
@Composable
fun SelaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SelaButtonStyle = SelaButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accent: Color = SelaPastel.blue,
    onAccent: Color = SelaPastel.blueDeep,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) SelaMotion.pressScaleDown else 1f,
        animationSpec = SelaMotion.bouncySpring(),
        label = "sela-button-press",
    )

    val backgroundBrush: Brush = when (style) {
        SelaButtonStyle.Primary -> SelaPastelGradients.auroraBlueLavender
        SelaButtonStyle.Soft -> Brush.linearGradient(
            listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.20f)),
        )
    }
    val contentColor: Color = when (style) {
        SelaButtonStyle.Primary -> SelaPastel.ink
        SelaButtonStyle.Soft -> onAccent
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pastelShadow(
                elevation = if (pressed) SelaPastelShadows.soft else SelaPastelShadows.card,
                shape = SelaPastelShapes.pill,
                color = if (style == SelaButtonStyle.Primary) SelaPastel.lavenderDeep else SelaPastelShadows.shadowColor,
            )
            .clip(SelaPastelShapes.pill)
            .background(backgroundBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
