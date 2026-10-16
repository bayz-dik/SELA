package id.sela.designsystem.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaV7
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Maskot SELA V7 — blob hijau "Plopp".
 *
 * Digambar vector via Canvas agar proporsi konsisten di semua ukuran:
 * - Badan hijau membulat dengan dua tonjolan jambul di atas
 * - Mata putih besar, pupil navy dengan highlight putih
 * - Mata bisa melirik (glance) dan berkedip (blink)
 * - Menghormati reduced-motion: jika aktif, mata statis tanpa animasi
 *
 * @param size ukuran keseluruhan maskot
 * @param glance arah lirikan mata (-1f kiri .. 1f kanan), null = animasi otomatis
 * @param withHeadphones tampilkan headphone (untuk layar Music)
 */
@Composable
fun V7Mascot(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    glance: Float? = null,
    withHeadphones: Boolean = false,
) {
    val context = LocalContext.current
    // Hormati reduced-motion: cek system animator scale
    val reducedMotion = remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }

    // Kedipan: 0f = terbuka penuh, 1f = tertutup
    val blinkAnim = remember { Animatable(0f) }
    // Lirikan otomatis
    var autoGlance by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) return@LaunchedEffect
        while (true) {
            // Tunggu 2.5–5 detik lalu kedip
            delay(Random.nextLong(2500, 5000))
            // Animasi kedip cepat: tutup-buka
            blinkAnim.animateTo(1f, animationSpec = tween(90))
            blinkAnim.animateTo(0f, animationSpec = tween(120))
            // Kadang melirik
            if (Random.nextFloat() < 0.4f) {
                autoGlance = Random.nextFloat() * 1.2f - 0.6f
                delay(1200)
                autoGlance = 0f
            }
        }
    }

    val blink = blinkAnim.value
    val currentGlance = glance ?: autoGlance

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawMascotBody(w, h)
        drawMascotEyes(w, h, blinkProgress = blink, glanceX = currentGlance)
        if (withHeadphones) drawHeadphones(w, h)
    }
}

private fun DrawScope.drawMascotBody(w: Float, h: Float) {
    val bodyColor = SelaV7.mascotBody
    val bodyDark = SelaV7.mascotBodyDark

    // Badan utama: oval membulat
    drawOval(
        color = bodyColor,
        topLeft = Offset(w * 0.08f, h * 0.18f),
        size = Size(w * 0.84f, h * 0.82f),
    )
    // Jambul: dua tonjolan di atas
    drawCircle(
        color = bodyColor,
        radius = w * 0.13f,
        center = Offset(w * 0.38f, h * 0.16f),
    )
    drawCircle(
        color = bodyColor,
        radius = w * 0.10f,
        center = Offset(w * 0.58f, h * 0.13f),
    )
    // Bayangan bawah halus
    drawOval(
        color = bodyDark.copy(alpha = 0.35f),
        topLeft = Offset(w * 0.18f, h * 0.78f),
        size = Size(w * 0.64f, h * 0.20f),
    )
}

private fun DrawScope.drawMascotEyes(
    w: Float,
    h: Float,
    blinkProgress: Float,
    glanceX: Float,
) {
    val eyeY = h * 0.52f
    val eyeRadiusX = w * 0.20f
    val eyeRadiusY = h * 0.20f * (1f - blinkProgress * 0.92f)
    if (eyeRadiusY < h * 0.015f) return // mata tertutup penuh

    val leftEyeX = w * 0.32f
    val rightEyeX = w * 0.68f

    // Putih mata
    drawOval(
        color = SelaV7.mascotEyeWhite,
        topLeft = Offset(leftEyeX - eyeRadiusX, eyeY - eyeRadiusY),
        size = Size(eyeRadiusX * 2, eyeRadiusY * 2),
    )
    drawOval(
        color = SelaV7.mascotEyeWhite,
        topLeft = Offset(rightEyeX - eyeRadiusX, eyeY - eyeRadiusY),
        size = Size(eyeRadiusX * 2, eyeRadiusY * 2),
    )

    if (blinkProgress > 0.5f) return // pupil hilang saat kedip

    // Pupil navy dengan lirikan
    val pupilR = w * 0.095f
    val glanceOffset = glanceX * w * 0.05f
    val pupilY = eyeY + h * 0.02f

    drawCircle(
        color = SelaV7.mascotPupil,
        radius = pupilR,
        center = Offset(leftEyeX + glanceOffset, pupilY),
    )
    drawCircle(
        color = SelaV7.mascotPupil,
        radius = pupilR,
        center = Offset(rightEyeX + glanceOffset, pupilY),
    )
    // Highlight putih
    val hlR = w * 0.032f
    drawCircle(
        color = Color.White,
        radius = hlR,
        center = Offset(leftEyeX + glanceOffset - pupilR * 0.3f, pupilY - pupilR * 0.35f),
    )
    drawCircle(
        color = Color.White,
        radius = hlR,
        center = Offset(rightEyeX + glanceOffset - pupilR * 0.3f, pupilY - pupilR * 0.35f),
    )

    // Alis
    val browY = eyeY - eyeRadiusY - h * 0.06f
    drawLine(
        color = SelaV7.mascotPupil,
        start = Offset(leftEyeX - w * 0.09f, browY),
        end = Offset(leftEyeX + w * 0.09f, browY - h * 0.015f),
        strokeWidth = w * 0.028f,
    )
    drawLine(
        color = SelaV7.mascotPupil,
        start = Offset(rightEyeX - w * 0.09f, browY - h * 0.015f),
        end = Offset(rightEyeX + w * 0.09f, browY),
        strokeWidth = w * 0.028f,
    )
}

private fun DrawScope.drawHeadphones(w: Float, h: Float) {
    val navy = SelaV7.ink
    // Band atas
    drawArc(
        color = navy,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.12f, h * 0.02f),
        size = Size(w * 0.76f, h * 0.5f),
    )
    // Ear cups
    drawOval(
        color = navy,
        topLeft = Offset(w * 0.02f, h * 0.42f),
        size = Size(w * 0.16f, h * 0.28f),
    )
    drawOval(
        color = navy,
        topLeft = Offset(w * 0.82f, h * 0.42f),
        size = Size(w * 0.16f, h * 0.28f),
    )
}
