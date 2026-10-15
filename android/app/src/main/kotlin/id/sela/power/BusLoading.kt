package id.sela.power

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animasi loading bergambar bus (fitur #106/#107).
 *
 * Digambar murni dengan Canvas — TANPA Lottie, tanpa aset eksternal: bodi bus
 * rounded-rect, jendela, lampu depan, dan dua roda yang berputar (rotate lewat
 * infiniteTransition) plus efek memantul ringan. Teks "Memuat..." di bawahnya.
 *
 * JUJUR: ini ilustrasi dekoratif supaya layar loading tidak kosong. Animasi ini
 * TIDAK mencerminkan progres pemuatan data yang sebenarnya — jangan dipakai
 * sebagai indikator progres.
 */
@Composable
fun BusLoadingAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bus-loading")
    val wheelRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "putaran-roda",
    )
    val bob by transition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "lompatan-bus",
    )

    val busColor = MaterialTheme.colorScheme.primary
    val glassColor = MaterialTheme.colorScheme.surface
    val tireColor = MaterialTheme.colorScheme.onSurface
    val hubColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(180.dp, 110.dp).offset(y = bob.dp)) {
            val w = size.width
            val h = size.height
            val bodyW = w * 0.82f
            val bodyH = h * 0.46f
            val bodyLeft = (w - bodyW) / 2f
            val bodyTop = h * 0.14f

            // Bodi bus
            drawRoundRect(
                color = busColor,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(20f, 20f),
            )
            // Jendela depan (kaca)
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(bodyLeft + bodyW * 0.62f, bodyTop + bodyH * 0.16f),
                size = Size(bodyW * 0.28f, bodyH * 0.38f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            // Jendela penumpang
            drawRoundRect(
                color = glassColor,
                topLeft = Offset(bodyLeft + bodyW * 0.08f, bodyTop + bodyH * 0.16f),
                size = Size(bodyW * 0.46f, bodyH * 0.38f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            // Lampu depan
            drawCircle(
                color = glassColor,
                radius = bodyH * 0.09f,
                center = Offset(bodyLeft + bodyW * 0.9f, bodyTop + bodyH * 0.82f),
            )
            // Roda: ban + jari-jari berputar
            val wheelR = h * 0.13f
            val wheelY = bodyTop + bodyH + wheelR * 0.35f
            listOf(bodyLeft + bodyW * 0.24f, bodyLeft + bodyW * 0.74f).forEach { cx ->
                drawCircle(color = tireColor, radius = wheelR, center = Offset(cx, wheelY))
                rotate(degrees = wheelRotation, pivot = Offset(cx, wheelY)) {
                    repeat(4) { i ->
                        val a = Math.toRadians(i * 45.0)
                        drawLine(
                            color = hubColor,
                            start = Offset(
                                cx + wheelR * 0.55f * cos(a).toFloat(),
                                wheelY + wheelR * 0.55f * sin(a).toFloat(),
                            ),
                            end = Offset(
                                cx - wheelR * 0.55f * cos(a).toFloat(),
                                wheelY - wheelR * 0.55f * sin(a).toFloat(),
                            ),
                            strokeWidth = wheelR * 0.16f,
                        )
                    }
                }
                drawCircle(color = hubColor, radius = wheelR * 0.2f, center = Offset(cx, wheelY))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Memuat...", style = MaterialTheme.typography.bodyLarge)
    }
}
