package id.sela.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradien aurora — campuran rona pastel yang mengalir. Dipakai untuk
 * latar hero, tombol primer, dan aksen dekoratif. Semua berhenti di
 * warna pastel; teks di atasnya selalu [SelaPastel.ink] (gelap).
 */
object SelaPastelGradients {
    val auroraBlueLavender: Brush
        get() = Brush.linearGradient(listOf(SelaPastel.blue, SelaPastel.lavender, SelaPastel.purple))

    val auroraMintCream: Brush
        get() = Brush.linearGradient(listOf(SelaPastel.mint, SelaPastel.mintSoft, SelaPastel.cream))

    val auroraPurplePink: Brush
        get() = Brush.linearGradient(listOf(SelaPastel.purple, SelaPastel.lavender, SelaPastel.pink))

    val sunnyPeach: Brush
        get() = Brush.linearGradient(listOf(SelaPastel.sunny, SelaPastel.peach))

    val skyBreeze: Brush
        get() = Brush.linearGradient(listOf(SelaPastel.blueSoft, SelaPastel.mintSoft))

    /** Gradien radial satu warna yang memudar ke transparan. */
    fun radialTint(color: Color, alpha: Float = 0.55f): Brush =
        Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)))
}

/**
 * Latar aurora: warm off-white dengan empat blob pastel lembut
 * (biru, lavender, mint, pink) yang memudar — kesan langit pagi.
 * Ditaruh paling bawah di layar hero / empty state.
 */
fun Modifier.auroraBackground(
    base: Color = SelaPastel.background,
): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    data class Blob(val center: Offset, val radius: Float, val color: Color)
    val blobs = listOf(
        Blob(Offset(w * 0.12f, h * 0.10f), w * 0.55f, SelaPastel.blue),
        Blob(Offset(w * 0.92f, h * 0.28f), w * 0.60f, SelaPastel.lavender),
        Blob(Offset(w * 0.75f, h * 0.88f), w * 0.65f, SelaPastel.mint),
        Blob(Offset(w * 0.10f, h * 0.95f), w * 0.50f, SelaPastel.pink),
    )
    onDrawBehind {
        drawRect(base)
        blobs.forEach { blob ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob.color.copy(alpha = 0.32f), Color.Transparent),
                    center = blob.center,
                    radius = blob.radius,
                ),
                radius = blob.radius,
                center = blob.center,
            )
        }
    }
}
