package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.SelaV7
import id.sela.designsystem.components.V7Mascot
import kotlinx.coroutines.delay

/**
 * Splash V7 — sesuai 01_SPLASH_ACUAN.png.
 * Logo SELA di tengah, lingkaran lime besar pojok kiri, maskot muncul dari bawah.
 */
@Composable
fun V7SplashScreen(
    onTimeout: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(1800)
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
    ) {
        // Lingkaran dekoratif lime pojok kiri atas
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(220.dp).align(Alignment.TopStart).offset((-60).dp, (-40).dp)
        ) {
            drawCircle(color = SelaV7.lime, radius = size.minDimension / 2)
        }
        // Lingkaran periwinkle kanan
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(140.dp).align(Alignment.TopEnd).offset(40.dp, 60.dp)
        ) {
            drawCircle(color = SelaV7.peri, radius = size.minDimension / 2)
        }

        // Logo tengah
        Text(
            text = "SELA",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black,
            fontSize = 56.sp,
            color = SelaV7.ink,
            modifier = Modifier.align(Alignment.Center),
        )

        // Maskot dari bawah
        V7Mascot(
            size = 280.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 60.dp),
        )
    }
}
