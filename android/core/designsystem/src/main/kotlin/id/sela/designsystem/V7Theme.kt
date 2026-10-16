package id.sela.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Tema SELA V7 — "Plopp".
 *
 * Satu-satunya tema untuk redesign total V7: krem hangat #FFFDF1,
 * lime #A9F500, navy #06133E, periwinkle #91A6FF.
 * Menggantikan SelaPastelTheme ("Pagi Cerah").
 */
@Composable
fun SelaV7Theme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SelaV7Scheme,
        typography = SelaPastelTypography,
        shapes = SelaPastelMaterialShapes,
        content = content,
    )
}
