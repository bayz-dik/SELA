package id.sela.v7

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.R
import id.sela.designsystem.SelaV7

/**
 * Login x Threads — dark mode.
 * Tombol pill PUTIH teks HITAM (primary), link biru (sekunder).
 * Sesuai Figma "SELA x Threads" 02 Login. Tanpa maskot/dekorasi.
 */
@Composable
fun V7LoginScreen(
    onGoogleSignIn: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SelaV7.background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(120.dp))
            Text(
                text = "SELA",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = SelaV7.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Teman perjalanan setiap hari",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                color = SelaV7.inkSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))

            // Tombol Google — pill PUTIH teks HITAM (ala Threads)
            Button(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = SelaV7.surfaceDim,
                    disabledContentColor = SelaV7.inkFaint,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google_g),
                        contentDescription = "Logo Google",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Lanjut dengan Google",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(
                text = "atau",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 14.sp,
                color = SelaV7.inkFaint,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            // Tombol sekunder — teks biru borderless (ala Threads)
            TextButton(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
            ) {
                Text(
                    text = "Masuk dengan Email",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = SelaV7.blue,
                )
            }

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaV7.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Belum punya akun? ",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 14.sp,
                    color = SelaV7.inkFaint,
                )
                TextButton(
                    onClick = onGoogleSignIn,
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(
                        text = "Daftar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = SelaV7.ink,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
