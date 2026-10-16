package id.sela.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema SELA x Threads — hasil reverse engineering APK Threads v451.
 *
 * Dark mode: PURE BLACK #000000 (default).
 * Light mode: putih #FFFFFF.
 * Aksen: biru #0095F6. Font: IGUIBetav9.
 *
 * @param darkMode true = tema gelap (default, sesuai desain Figma).
 */
@Composable
fun SelaV7Theme(
    darkMode: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Sinkronkan palet SelaV7 — assignment volatile idempoten, aman dipanggil
    // saat composition (tidak memicu recomposition karena SelaV7.* bukan State).
    SelaV7.setDarkMode(darkMode)
    MaterialTheme(
        colorScheme = if (darkMode) SelaV7DarkScheme else SelaV7Scheme,
        typography = ThreadsTypography,
        shapes = ThreadsMaterialShapes,
        content = content,
    )
}
