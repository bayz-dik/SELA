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

    // Badan utama: oval membulat (proporsi referensi: lebar, pipi penuh)
    drawOval(
        color = bodyColor,
        topLeft = Offset(w * 0.06f, h * 0.20f),
        size = Size(w * 0.88f, h * 0.80f),
    )
    // Jambul: dua tonjolan kecil berdekatan di tengah atas (sesuai referensi)
    drawCircle(
        color = bodyColor,
        radius = w * 0.085f,
        center = Offset(w * 0.45f, h * 0.185f),
    )
    drawCircle(
        color = bodyColor,
        radius = w * 0.07f,
        center = Offset(w * 0.56f, h * 0.175f),
    )
    // Bayangan bawah halus
    drawOval(
        color = bodyDark.copy(alpha = 0.30f),
        topLeft = Offset(w * 0.16f, h * 0.80f),
        size = Size(w * 0.68f, h * 0.18f),
    )
}

private fun DrawScope.drawMascotEyes(
    w: Float,
    h: Float,
    blinkProgress: Float,
    glanceX: Float,
) {
    // Mata BESAR sesuai referensi: oval putih raksasa, pupil navy besar di tengah-bawah
    val eyeY = h * 0.52f
    val eyeRadiusX = w * 0.235f
    val eyeRadiusY = h * 0.235f * (1f - blinkProgress * 0.92f)
    if (eyeRadiusY < h * 0.015f) return // mata tertutup penuh

    val leftEyeX = w * 0.30f
    val rightEyeX = w * 0.70f

    // Putih mata — oval besar
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

    // Pupil navy besar dengan lirikan
    val pupilR = w * 0.115f
    val glanceOffset = glanceX * w * 0.06f
    val pupilY = eyeY + h * 0.035f

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
    // Highlight putih (khas referensi: titik terang di pupil)
    val hlR = w * 0.030f
    drawCircle(
        color = Color.White,
        radius = hlR,
        center = Offset(leftEyeX + glanceOffset - pupilR * 0.28f, pupilY - pupilR * 0.32f),
    )
    drawCircle(
        color = Color.White,
        radius = hlR,
        center = Offset(rightEyeX + glanceOffset - pupilR * 0.28f, pupilY - pupilR * 0.32f),
    )

    // Alis melengkung (arc kecil di atas tiap mata, sesuai referensi)
    val browY = eyeY - eyeRadiusY - h * 0.045f
    drawArc(
        color = SelaV7.mascotPupil,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(leftEyeX - w * 0.075f, browY - h * 0.02f),
        size = Size(w * 0.15f, h * 0.05f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.026f),
    )
    drawArc(
        color = SelaV7.mascotPupil,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(rightEyeX - w * 0.075f, browY - h * 0.02f),
        size = Size(w * 0.15f, h * 0.05f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.026f),
    )

    // Hidung: dua titik kecil di antara/bawah mata (sesuai referensi)
    val noseY = pupilY + pupilR + h * 0.045f
    val noseR = w * 0.016f
    drawCircle(
        color = SelaV7.mascotPupil,
        radius = noseR,
        center = Offset(w * 0.5f - w * 0.028f, noseY),
    )
    drawCircle(
        color = SelaV7.mascotPupil,
        radius = noseR,
        center = Offset(w * 0.5f + w * 0.028f, noseY),
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
