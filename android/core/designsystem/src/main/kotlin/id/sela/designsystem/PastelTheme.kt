package id.sela.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema pastel SELA — "Pagi Cerah".
 *
 * Satu-satunya tema untuk redesign total: light pastel di atas warm
 * off-white, menggantikan [SelaTheme] ("Malam Kota"). Tidak ada varian
 * gelap — dunia pastel SELA selalu cerah.
 */
@Composable
fun SelaPastelTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SelaPastelScheme,
        typography = SelaPastelTypography,
        shapes = SelaPastelMaterialShapes,
        content = content,
    )
}
