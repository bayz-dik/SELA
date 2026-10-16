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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.SelaV7
import kotlinx.coroutines.delay

/**
 * Splash x Threads — dark mode minimal.
 * Background pure black, wordmark "SELA" putih di tengah.
 * Sesuai Figma "SELA x Threads" 01 Splash.
 */
@Composable
fun V7SplashScreen(
    onTimeout: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(1500)
        onTimeout()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SELA",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            color = SelaV7.ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "dari Bayu",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 13.sp,
            color = SelaV7.inkFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
        )
    }
}
