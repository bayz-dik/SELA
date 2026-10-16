package id.sela.v7

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.SelaV7

/**
 * Loading x Threads — dark mode minimal.
 * Wordmark + progress bar tipis. Sesuai Figma 03 Loading.
 */
@Composable
fun V7LoadingScreen(
    message: String = "Memuat...",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "SELA",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = SelaV7.ink,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = SelaV7.ink,
                trackColor = SelaV7.hairline,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = SelaV7.inkFaint,
                textAlign = TextAlign.Center,
            )
        }
    }
}
