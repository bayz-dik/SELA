package id.sela.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes Threads — dari reverse APK v451.
 * Radius scale: 4/8/12/16dp + pill. Card utama 16dp. NOL shadow.
 */
object ThreadsShapes {
    /** Elemen kecil. */
    val xs = RoundedCornerShape(4.dp)
    /** Chip, thumbnail. */
    val sm = RoundedCornerShape(8.dp)
    /** Card medium. */
    val md = RoundedCornerShape(12.dp)
    /** Card utama (tersering di Threads: 16dp). */
    val lg = RoundedCornerShape(16.dp)
    /** Tombol, chip — selalu pill penuh. */
    val pill = RoundedCornerShape(percent = 50)
    /** Avatar — selalu bulat sempurna. */
    val avatar = CircleShape
    /** Bottom sheet. */
    val sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    /** Input field. */
    val field = RoundedCornerShape(22.dp)
}

/** Shapes Material untuk [SelaV7Theme]. */
val ThreadsMaterialShapes = ThreadsShapes.let {
    Shapes(
        extraSmall = it.xs,
        small = it.sm,
        medium = it.md,
        large = it.lg,
        extraLarge = it.pill,
    )
}

/** Alias agar V7Theme bisa pakai nama singkat. */
val ThreadsShapesMaterial = ThreadsMaterialShapes
