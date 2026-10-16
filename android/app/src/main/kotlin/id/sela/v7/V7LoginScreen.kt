package id.sela.v7

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.sela.designsystem.SelaV7
import id.sela.designsystem.components.V7Mascot

/**
 * Login V7 — sesuai 02_LOGIN_ACUAN.png.
 * Headline + deskripsi, tombol Google navy, tombol outline "Sudah punya akun? Masuk",
 * maskot mengintip dari bawah (dinaikkan, tidak tenggelam).
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
        // Dekorasi: lingkaran lime kiri atas
        Canvas(
            modifier = Modifier.size(180.dp).align(Alignment.TopStart).offset((-50).dp, (-30).dp)
        ) {
            drawCircle(color = SelaV7.lime, radius = size.minDimension / 2)
        }
        // Lingkaran periwinkle kanan
        Canvas(
            modifier = Modifier.size(110.dp).align(Alignment.TopEnd).offset(30.dp, 80.dp)
        ) {
            drawCircle(color = SelaV7.peri, radius = size.minDimension / 2)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 120.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Yuk, mulai\nperjalananmu!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                color = SelaV7.ink,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Cari rute, pantau bus, dan temukan perjalanan yang lebih mudah.",
                style = MaterialTheme.typography.bodyMedium,
                color = SelaV7.inkSoft,
            )
            Spacer(Modifier.height(28.dp))

            // Tombol Google navy
            Button(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SelaV7.ink,
                    contentColor = Color.White,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    // Logo G sederhana
                    Text(
                        text = "G",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    Text(
                        text = "Mulai dengan Google",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Tombol outline
            OutlinedButton(
                onClick = onGoogleSignIn,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SelaV7.limeDeep),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, SelaV7.lime),
            ) {
                Text(
                    text = "Sudah punya akun? Masuk",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
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
        }

        // Maskot mengintip dari bawah — dinaikkan 4-8% viewport
        V7Mascot(
            size = 300.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 90.dp), // hanya mata + jambul terlihat, tidak tenggelam
        )
    }
}
