package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Pil kaca (frosted-glass) — untuk navigasi melayang di atas peta.
 * Putih tembus pandang + garis tepi terang + bayangan lembut.
 *
 * Catatan: blur latar asli (backdrop blur) butuh RenderEffect (API 31+)
 * dan dipasang di level container; versi ini memakai translusensi yang
 * aman di semua API.
 */
@Composable
fun GlassPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    icon: ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.pastelShadow(
            elevation = SelaPastelShadows.soft,
            shape = SelaPastelShapes.pill,
        ),
        shape = SelaPastelShapes.pill,
        color = if (selected) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) SelaPastel.blueDeep else SelaPastel.inkSoft,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SelaPastel.ink else SelaPastel.inkSoft,
            )
        }
    }
}

/**
 * Wadah navigasi kaca — barisan [GlassPill] dalam satu pil kaca besar,
 * untuk bottom nav / switcher melayang di atas peta.
 */
@Composable
fun GlassPillNavBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.pastelShadow(
            elevation = SelaPastelShadows.lifted,
            shape = SelaPastelShapes.pill,
        ),
        shape = SelaPastelShapes.pill,
        color = Color.White.copy(alpha = 0.60f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.70f)),
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
