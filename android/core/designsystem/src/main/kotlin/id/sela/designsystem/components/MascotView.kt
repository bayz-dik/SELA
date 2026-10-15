package id.sela.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Maskot original SELA — "SelaBlob".
 *
 * Blob pastel dengan wajah cute, digambar 100% dengan Compose Canvas
 * (tanpa aset gambar). Siap diganti Lottie/Rive nanti — API [expression]
 * tetap sama.
 */
enum class SelaMascotExpression {
    Happy,
    Thinking,
    Sleeping,
}

@Composable
fun SelaMascot(
    expression: SelaMascotExpression = SelaMascotExpression.Happy,
    modifier: Modifier = Modifier,
    diameter: Dp = 120.dp,
    bodyColor: Color = SelaPastel.mint,
    animate: Boolean = true,
    contentDescription: String? = null,
) {
    val idle = rememberInfiniteTransition(label = "sela-mascot-idle")
    val bob by idle.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sela-mascot-bob",
    )
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .size(diameter)
            .offset { IntOffset(0, if (animate) (bob * 10f).roundToInt() else 0) }
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val s = size.minDimension
            val cx = s / 2f
            val cy = s / 2f
            val r = s * 0.40f
            val ink = SelaPastel.ink

            // Bayangan lembut di bawah blob
            drawOval(
                color = ink.copy(alpha = 0.08f),
                topLeft = Offset(cx - r * 0.78f, cy + r * 1.04f),
                size = androidx.compose.ui.geometry.Size(r * 1.56f, r * 0.26f),
            )

            // Badan blob — gumpalan organik (bukan lingkaran sempurna)
            val edge = Color(
                red = bodyColor.red * 0.80f,
                green = bodyColor.green * 0.80f,
                blue = bodyColor.blue * 0.80f,
            )
            drawPath(
                path = blobPath(Offset(cx, cy), r),
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.45f), bodyColor, edge),
                    center = Offset(cx - r * 0.35f, cy - r * 0.40f),
                    radius = r * 2.1f,
                ),
            )
            drawPath(
                path = blobPath(Offset(cx, cy), r),
                color = edge.copy(alpha = 0.35f),
                style = Stroke(width = s * 0.012f),
            )

            // Pipi merona
            val blush = SelaPastel.pink.copy(alpha = 0.55f)
            drawOval(
                color = blush,
                topLeft = Offset(cx - r * 0.72f, cy + r * 0.05f),
                size = androidx.compose.ui.geometry.Size(r * 0.30f, r * 0.18f),
            )
            drawOval(
                color = blush,
                topLeft = Offset(cx + r * 0.42f, cy + r * 0.05f),
                size = androidx.compose.ui.geometry.Size(r * 0.30f, r * 0.18f),
            )

            when (expression) {
                SelaMascotExpression.Happy -> drawHappyFace(cx, cy, r, ink)
                SelaMascotExpression.Thinking -> drawThinkingFace(cx, cy, r, ink)
                SelaMascotExpression.Sleeping -> drawSleepingFace(cx, cy, r, ink, textMeasurer)
            }
        }
    }
}

/** Gumpalan organik: titik-titik dengan radius berdenyut, disambung mulus. */
private fun DrawScope.blobPath(center: Offset, radius: Float): Path {
    val steps = 64
    val points = List(steps) { i ->
        val a = (i / steps.toFloat()) * (2f * PI.toFloat())
        val wobble = 1f + 0.07f * sin(3f * a + 0.8f) + 0.04f * sin(5f * a + 2.1f)
        val rr = radius * wobble
        Offset(center.x + rr * cos(a), center.y + rr * sin(a))
    }
    fun mid(a: Offset, b: Offset) = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    return Path().apply {
        val start = mid(points[0], points[1])
        moveTo(start.x, start.y)
        for (i in 1..steps) {
            val p = points[i % steps]
            val m = mid(p, points[(i + 1) % steps])
            quadraticTo(p.x, p.y, m.x, m.y)
        }
        close()
    }
}

private fun DrawScope.drawHappyFace(cx: Float, cy: Float, r: Float, ink: Color) {
    val er = r * 0.115f
    val eyeY = cy - r * 0.10f
    listOf(cx - r * 0.36f, cx + r * 0.36f).forEach { ex ->
        // Mata bulat + kilau
        drawCircle(ink, er, Offset(ex, eyeY))
        drawCircle(Color.White, er * 0.38f, Offset(ex - er * 0.30f, eyeY - er * 0.35f))
    }
    // Senyum lebar terbuka
    drawArc(
        color = ink,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = true,
        topLeft = Offset(cx - r * 0.31f, cy + r * 0.10f),
        size = androidx.compose.ui.geometry.Size(r * 0.62f, r * 0.50f),
    )
    // Lidah
    drawOval(
        color = SelaPastel.pink,
        topLeft = Offset(cx - r * 0.15f, cy + r * 0.40f),
        size = androidx.compose.ui.geometry.Size(r * 0.30f, r * 0.16f),
    )
}

private fun DrawScope.drawThinkingFace(cx: Float, cy: Float, r: Float, ink: Color) {
    val er = r * 0.095f
    val eyeY = cy - r * 0.18f
    // Mata melirik ke kanan atas
    listOf(cx - r * 0.36f, cx + r * 0.36f).forEach { ex ->
        drawCircle(ink, er, Offset(ex + er * 0.30f, eyeY - er * 0.20f))
    }
    // Satu alis terangkat
    drawLine(
        color = ink,
        start = Offset(cx + r * 0.20f, cy - r * 0.42f),
        end = Offset(cx + r * 0.52f, cy - r * 0.50f),
        strokeWidth = r * 0.055f,
        cap = StrokeCap.Round,
    )
    // Mulut miring mikir
    drawLine(
        color = ink,
        start = Offset(cx - r * 0.14f, cy + r * 0.34f),
        end = Offset(cx + r * 0.16f, cy + r * 0.28f),
        strokeWidth = r * 0.055f,
        cap = StrokeCap.Round,
    )
    // Gelembung pikiran
    drawCircle(ink.copy(alpha = 0.30f), r * 0.070f, Offset(cx + r * 0.60f, cy - r * 0.72f))
    drawCircle(ink.copy(alpha = 0.30f), r * 0.105f, Offset(cx + r * 0.82f, cy - r * 0.94f))
}

private fun DrawScope.drawSleepingFace(
    cx: Float,
    cy: Float,
    r: Float,
    ink: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    val eyeY = cy - r * 0.10f
    // Mata tertutup (lengkung ke bawah)
    listOf(cx - r * 0.36f, cx + r * 0.36f).forEach { ex ->
        drawArc(
            color = ink,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(ex - r * 0.14f, eyeY - r * 0.10f),
            size = androidx.compose.ui.geometry.Size(r * 0.28f, r * 0.20f),
            style = Stroke(width = r * 0.055f, cap = StrokeCap.Round),
        )
    }
    // Mulut "o" kecil
    drawCircle(
        color = ink,
        radius = r * 0.065f,
        center = Offset(cx, cy + r * 0.32f),
        style = Stroke(width = r * 0.05f),
    )
    // "z Z" melayang
    val zStyle = TextStyle(
        color = SelaPastel.inkSoft,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
    )
    drawText(textMeasurer, "z", Offset(cx + r * 0.66f, cy - r * 1.04f), zStyle)
    drawText(
        textMeasurer,
        "z",
        Offset(cx + r * 0.88f, cy - r * 1.30f),
        zStyle.copy(fontSize = 30.sp),
    )
}

@Preview(name = "SelaBlob — semua ekspresi")
@Composable
private fun SelaMascotPreview() {
    SelaPastelTheme {
        Surface(color = SelaPastel.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelaMascot(SelaMascotExpression.Happy, animate = false)
                SelaMascot(
                    SelaMascotExpression.Thinking,
                    animate = false,
                    bodyColor = SelaPastel.lavender,
                )
                SelaMascot(
                    SelaMascotExpression.Sleeping,
                    animate = false,
                    bodyColor = SelaPastel.blue,
                )
            }
        }
    }
}
