package id.sela.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema SELA V7 — "Plopp".
 *
 * Krem hangat #FFFDF1, lime #A9F500, navy #06133E, periwinkle #91A6FF.
 * Mendukung mode gelap: [darkMode] mengganti skema Material3 sekaligus
 * palet [SelaV7] (latar/tinta/bubble) via [SelaV7.setDarkMode].
 *
 * @param darkMode true = tema gelap navy pekat dengan identitas lime tetap.
 */
@Composable
fun SelaV7Theme(
    darkMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Sinkronkan palet SelaV7 sebelum konten digambar — semua layar
    // membaca SelaV7.background/ink/dll saat recomposition.
    SelaV7.setDarkMode(darkMode)
    MaterialTheme(
        colorScheme = if (darkMode) SelaV7DarkScheme else SelaV7Scheme,
        typography = SelaPastelTypography,
        shapes = SelaPastelMaterialShapes,
        content = content,
    )
}
