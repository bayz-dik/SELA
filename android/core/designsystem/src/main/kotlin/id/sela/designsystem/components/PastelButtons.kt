package id.sela.designsystem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Tombol teks pastel — untuk aksi prioritas rendah
 * ("Hapus riwayat", "Ekspor CSV", "Ajak bareng"). Pasangan dari
 * [SelaButton] (aksi utama) di dunia pastel.
 */
@Composable
fun PastelTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    color: Color = if (destructive) SelaPastel.dangerDeep else SelaPastel.blueDeep,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = SelaPastelShapes.pill,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) color else SelaPastel.inkFaint,
        )
    }
}
