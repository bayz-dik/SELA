package id.sela.feature.auth

import android.animation.ValueAnimator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot

private val Navy = Color(0xFF08153F)
private val Lime = Color(0xFFA9ED00)
private val Cream = Color(0xFFFFFCEB)
private val Periwinkle = Color(0xFF8C9CFA)

/**
 * Layar welcome/login SELA.
 *
 * TUGAS A: Maskot hijau dinaikkan agar tidak terlalu rendah/terpotong.
 * - faceHeight: 34% viewport (naik dari 30%), min 180dp
 * - Menghormati WindowInsets.navigationBars (tidak tertutup gesture bar)
 * - Tidak hardcode pixel; proporsional terhadap viewport
 *
 * TUGAS B: Mata mengikuti jari (pointer tracking), blink acak, idle roaming.
 * Render native via Canvas (bukan GIF). Hormati reduce motion.
 */
@Composable
fun SelaLoginScreen(
    onGoogleSignIn: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    var loginPage by rememberSaveable { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    var pointerX by remember { mutableFloatStateOf(0f) }
    var pointerY by remember { mutableFloatStateOf(0f) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    val lookX = if (touched && screenSize.width > 0) {
        ((pointerX / screenSize.width) * 2f - 1f).coerceIn(-1f, 1f)
    } else 0f
    val lookY = if (touched && screenSize.height > 0) {
        ((pointerY / screenSize.height) * 2f - 1f).coerceIn(-1f, 1f)
    } else 0f

    Box(
        modifier = modifier.fillMaxSize()
            .background(if (loginPage) Lime else Cream)
            .onSizeChanged { screenSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val point = event.changes.firstOrNull()?.position ?: continue
                        touched = event.changes.any { it.pressed }
                        pointerX = point.x
                        pointerY = point.y
                    }
                }
            },
    ) {
        if (!loginPage) {
            WelcomeContents(
                onContinue = { loginPage = true },
                lookX = lookX,
                lookY = lookY,
            )
        } else {
            LoginContents(
                onBack = { loginPage = false },
                onGoogle = onGoogleSignIn,
                isLoading = isLoading,
                errorMessage = errorMessage,
                lookX = lookX,
                lookY = lookY,
            )
        }
    }
}

@Composable
private fun WelcomeContents(onContinue: () -> Unit, lookX: Float, lookY: Float) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // TUGAS A: faceHeight dinaikkan dari 30% ke 34% viewport agar maskot
        // tidak terlalu rendah/terpotong. Min 180dp, max 300dp.
        // windowInsetsPadding memastikan tidak tertutup navigation bar.
        val faceHeight = (maxHeight * .34f).coerceIn(180.dp, 300.dp)
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Lime, radius = size.width * .35f, center = Offset(size.width * .08f, 0f))
            drawCircle(Periwinkle, radius = size.width * .20f, center = Offset(size.width * 1.10f, size.height * .22f))
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 68.dp, start = 31.dp, end = 31.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text("SELA", color = Navy, fontWeight = FontWeight.Black, fontSize = 30.sp)
            Spacer(Modifier.height(58.dp))
            Text("Mau ke mana?", color = Navy, fontWeight = FontWeight.Black,
                fontSize = 39.sp, lineHeight = 42.sp, letterSpacing = (-1.7).sp)
            Spacer(Modifier.height(14.dp))
            Text("Cari rute dan pantau bus TransJakarta.",
                color = Color(0xFF526078), fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(27.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Navy),
            ) { Text("Mulai dengan Google", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        // TUGAS A: Maskot di-anchor ke bottom dengan padding untuk navigation bar,
        // height 34% memastikan kepala lebih terlihat (naik ~4% viewport).
        AnimatedSelaFace(
            lookX = lookX,
            lookY = lookY,
            backgroundHead = true,
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(faceHeight)
                .windowInsetsPadding(WindowInsets.navigationBars),
        )
    }
}

@Composable
private fun LoginContents(
    onBack: () -> Unit,
    onGoogle: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    lookX: Float,
    lookY: Float,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val faceHeight = (maxHeight * .36f).coerceIn(185.dp, 300.dp)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 35.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF678D00)),
            ) { Text("‹  Kembali", color = Navy, fontWeight = FontWeight.Bold) }
            AnimatedSelaFace(lookX, lookY, backgroundHead = false,
                modifier = Modifier.fillMaxWidth().height(faceHeight))
            Spacer(Modifier.height(12.dp))
            Text("Masuk", fontSize = 34.sp, lineHeight = 37.sp,
                fontWeight = FontWeight.Black, color = Navy, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Lanjut dengan akun Google.",
                fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
                color = Color(0xFF3C5114))
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onGoogle,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(57.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy),
                shape = RoundedCornerShape(17.dp),
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("G   Lanjutkan dengan Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(16.dp))
                Text(errorMessage, color = Color(0xFF772214), fontSize = 14.sp,
                    textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * TUGAS B: Maskot dengan mata yang benar-benar bergerak.
 * - Mata mengikuti posisi jari (lookX/lookY dari pointer tracking)
 * - Idle eye roaming (gerakan acak saat tidak disentuh)
 * - Blink acak via keyframes
 * - Render native Canvas (bukan GIF)
 * - Hormati reduce motion via ValueAnimator.areAnimatorsEnabled()
 */
@Composable
private fun AnimatedSelaFace(
    lookX: Float,
    lookY: Float,
    backgroundHead: Boolean,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val transition = rememberInfiniteTransition(label = "SELA eye animation")
    val idleX by transition.animateFloat(
        initialValue = -.45f, targetValue = .45f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(3300),
            repeatMode = RepeatMode.Reverse,
        ), label = "Eye roaming",
    )
    val blink by transition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4800
                1f at 0
                1f at 3450
                .08f at 3540
                1f at 3670
                1f at 4800
            }, repeatMode = RepeatMode.Restart,
        ), label = "Blink",
    )
    // Mata mengikuti sentuhan. Saat tidak disentuh, idle memberi gerakan kecil.
    val x = (lookX * .65f + if (motionEnabled) idleX * .35f else 0f).coerceIn(-1f, 1f)
    val y = lookY.coerceIn(-1f, 1f)
    Canvas(modifier) { drawSelaFace(x, y, if (motionEnabled) blink else 1f, backgroundHead) }
}

private fun DrawScope.drawSelaFace(x: Float, y: Float, blink: Float, fullHead: Boolean) {
    val w = size.width
    val h = size.height
    if (fullHead) {
        val headPath = Path().apply {
            moveTo(-w * .05f, h)
            lineTo(-w * .05f, h * .58f)
            cubicTo(w * .06f, h * .04f, w * .94f, h * .04f, w * 1.05f, h * .58f)
            lineTo(w * 1.05f, h)
            close()
        }
        drawPath(headPath, Lime)
    }
    val scale = if (fullHead) 1f else .98f
    val eyeY = if (fullHead) h * .67f else h * .62f
    val eyeRadius = (w * .18f * scale).coerceAtMost(h * .27f)
    val leftX = w * .295f
    val rightX = w * .705f

    val tuftY = if (fullHead) h * .15f else h * .11f
    drawOval(Cream, topLeft = Offset(w * .48f, tuftY), size = Size(w * .07f, h * .16f))
    drawOval(Cream, topLeft = Offset(w * .525f, tuftY - h * .03f), size = Size(w * .08f, h * .18f))

    listOf(leftX, rightX).forEachIndexed { index, cx ->
        val brow = Path().apply {
            moveTo(cx - eyeRadius * .55f, eyeY - eyeRadius * 1.43f)
            quadraticTo(cx - eyeRadius * .12f, eyeY - eyeRadius * 1.75f,
                cx + eyeRadius * .50f, eyeY - eyeRadius * 1.40f)
        }
        drawPath(brow, Navy, style = Stroke(width = eyeRadius * .16f, cap = StrokeCap.Round))
        drawOval(Color.White, topLeft = Offset(cx - eyeRadius, eyeY - eyeRadius * blink),
            size = Size(eyeRadius * 2, eyeRadius * 2 * blink))
        val pupilRadius = eyeRadius * .59f
        val px = cx + x * eyeRadius * .26f + pupilRadius * .17f
        val py = eyeY + y * eyeRadius * .22f + eyeRadius * .17f
        if (blink > .26f) {
            drawCircle(Navy, radius = pupilRadius, center = Offset(px, py))
            drawCircle(Color.White, radius = pupilRadius * .24f,
                center = Offset(px - pupilRadius * .36f, py - pupilRadius * .38f))
        }
    }
    val noseY = eyeY + eyeRadius * 1.13f
    drawCircle(Navy, radius = w * .012f, center = Offset(w * .48f, noseY))
    drawCircle(Navy, radius = w * .012f, center = Offset(w * .53f, noseY))
}
