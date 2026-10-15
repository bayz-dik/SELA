package id.sela.designsystem

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bayangan lembut untuk claymorphism — selalu berwarna (ungu keabuan),
 * tidak pernah hitam pekat. Kartu "mengambang" di atas warm off-white
 * seperti tanah liat, bukan melayang gelap.
 */
object SelaPastelShadows {
    val soft: Dp = 8.dp
    val card: Dp = 16.dp
    val lifted: Dp = 24.dp
    val floating: Dp = 32.dp

    /** Warna bayangan: ungu keabuan lembut, bukan hitam. */
    val shadowColor = Color(0xFF8A7BA8)
    val spotAlpha = 0.16f
    val ambientAlpha = 0.10f
}

/** Bayangan pastel berwarna di sekeliling [shape]. */
fun Modifier.pastelShadow(
    elevation: Dp = SelaPastelShadows.card,
    shape: Shape,
    color: Color = SelaPastelShadows.shadowColor,
): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    spotColor = color.copy(alpha = SelaPastelShadows.spotAlpha),
    ambientColor = color.copy(alpha = SelaPastelShadows.ambientAlpha),
)

/**
 * Basis claymorphism: bayangan luar lembut + latar solid mengikuti [shape].
 * Highlight "cahaya dari atas" ditambahkan oleh komponen (mis. [PastelCard])
 * sebagai lapisan dalam supaya ter-clip rapi di sudut membulat.
 */
fun Modifier.claymorphic(
    background: Color,
    shape: Shape,
    elevation: Dp = SelaPastelShadows.card,
    shadowTint: Color = SelaPastelShadows.shadowColor,
): Modifier = this
    .pastelShadow(elevation = elevation, shape = shape, color = shadowTint)
    .background(background, shape)
