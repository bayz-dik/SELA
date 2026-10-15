package id.sela.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Kartu pastel — tanah liat empuk: sudut membulat besar, bayangan lembut
 * berwarna, dan highlight cahaya dari atas.
 *
 * @param tint bila diisi, latar kartu bergradasi putih → [tint] lembut
 *        (untuk kartu berwarna seperti info bus). Bila null, putih polos.
 */
@Composable
fun PastelCard(
    modifier: Modifier = Modifier,
    background: Color = Color.White,
    tint: Color? = null,
    shape: Shape = SelaPastelShapes.lg,
    elevation: Dp = SelaPastelShadows.card,
    shadowTint: Color = SelaPastelShadows.shadowColor,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .pastelShadow(elevation = elevation, shape = shape, color = shadowTint)
            .background(
                brush = if (tint != null) {
                    Brush.verticalGradient(
                        0f to Color.White,
                        1f to tint.copy(alpha = 0.45f),
                    )
                } else {
                    Brush.linearGradient(listOf(background, background))
                },
                shape = shape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .clip(shape),
    ) {
        // Highlight "cahaya dari atas" — lapisan dalam, ter-clip rapi.
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val highlight = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.35f),
                        0.4f to Color.Transparent,
                        startY = 0f,
                        endY = size.height,
                    )
                    onDrawBehind { drawRect(highlight) }
                },
        )
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
