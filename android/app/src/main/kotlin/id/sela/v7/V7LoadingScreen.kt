package id.sela.v7

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaV7
import id.sela.designsystem.components.V7Mascot

/**
 * Loading V7 — sesuai 03_LOADING_ACUAN.png.
 * Maskot melayang (animasi float), progress bar lime di bawah.
 */
@Composable
fun V7LoadingScreen(
    message: String = "Memuat...",
) {
    // Animasi melayang
    val infinite = rememberInfiniteTransition(label = "float")
    val offsetY by infinite.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatY",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        // Lingkaran dekoratif bawah
        Canvas(
            modifier = Modifier.size(300.dp).align(Alignment.BottomStart).offset((-80).dp, 80.dp)
        ) {
            drawCircle(color = SelaV7.lime, radius = size.minDimension / 2)
        }
        Canvas(
            modifier = Modifier.size(200.dp).align(Alignment.BottomEnd).offset(60.dp, 40.dp)
        ) {
            drawCircle(color = SelaV7.peri, radius = size.minDimension / 2)
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            V7Mascot(
                size = 160.dp,
                modifier = Modifier.offset(y = offsetY.dp),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(200.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = SelaV7.lime,
                    trackColor = SelaV7.limeSoft,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaV7.inkSoft,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
