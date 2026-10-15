package id.sela.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Bentuk dunia pastel — semuanya membulat besar. Tidak ada sudut tajam:
 * kartu, tombol, dan sel bento memakai radius 16–40dp, elemen kecil
 * memakai pill penuh.
 */
object SelaPastelShapes {
    val xs = RoundedCornerShape(12.dp)
    val sm = RoundedCornerShape(16.dp)
    val md = RoundedCornerShape(24.dp)
    val lg = RoundedCornerShape(32.dp)
    val xl = RoundedCornerShape(40.dp)

    /** Tombol, chip, dan nav — selalu pill penuh. */
    val pill = RoundedCornerShape(percent = 50)

    /** Sel bento grid. */
    val bento = RoundedCornerShape(28.dp)

    /** Bottom sheet / panel bawah. */
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    /** Bidang input. */
    val field = RoundedCornerShape(20.dp)
}

/** Shapes Material untuk [SelaPastelTheme]. */
val SelaPastelMaterialShapes = Shapes(
    extraSmall = SelaPastelShapes.xs,
    small = SelaPastelShapes.sm,
    medium = SelaPastelShapes.md,
    large = SelaPastelShapes.lg,
    extraLarge = SelaPastelShapes.xl,
)
