package id.sela.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * SELA "Malam Kota" — sistem desain dark-first.
 *
 * Latar gelap malam + aksen volt (chartreuse elektrik) untuk elemen live.
 * Permukaan memakai garis rambut (hairline), bukan bayangan berat.
 * Mengikuti tema sistem: gelap = Malam Kota, terang = Siang.
 */
@Deprecated(
    "Redesign pastel 2026-10: gunakan SelaPastelTheme. " +
        "Tema Malam Kota dipertahankan sementara untuk layar lama.",
)
object SelaTokens {
    // Bentuk
    val pill = RoundedCornerShape(percent = 50)
    val card = RoundedCornerShape(24.dp)
    val cardSmall = RoundedCornerShape(18.dp)
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val field = RoundedCornerShape(20.dp)
    val marker = RoundedCornerShape(12.dp)

    // Jarak — skala 4/8
    val spaceSmall = 4.dp
    val spaceMedium = 8.dp
    val spaceLarge = 16.dp
    val spaceSection = 24.dp

    // Elevasi — hemat, hanya untuk panel melayang di atas peta
    val floatElevation: Dp = 12.dp
    val cardElevation: Dp = 0.dp

    // Ukuran sentuh & panel
    val touchTarget: Dp = 48.dp
    val sheetPeek: Dp = 0.dp
}

@Deprecated(
    "Redesign pastel 2026-10: gunakan SelaPastelTheme. " +
        "Tema Malam Kota dipertahankan sementara untuk layar lama.",
)
@Composable
fun SelaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /**
     * Hitam murni untuk layar AMOLED (fitur #60). Hanya berlaku saat tema
     * gelap; bila false perilaku sama seperti sebelumnya.
     */
    amoled: Boolean = false,
    /**
     * Warna dinamis Material You dari wallpaper perangkat (fitur #91,
     * Android 12+). Di perangkat lebih lama otomatis diabaikan.
     */
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        amoled && darkTheme -> SelaAmoledScheme
        else -> if (darkTheme) SelaDarkScheme else SelaLightScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SelaTypography,
        shapes = Shapes(
            extraSmall = SelaTokens.cardSmall,
            small = SelaTokens.cardSmall,
            medium = SelaTokens.card,
            large = SelaTokens.card,
            extraLarge = SelaTokens.sheet,
        ),
        content = content,
    )
}
