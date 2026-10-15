package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Tombol primer SELA — volt penuh, teks gelap. Untuk aksi utama.
 * Komponen #16 (Primary Button).
 */
@Composable
fun SelaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 56.dp),
        shape = SelaTokens.pill,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SelaPalette.volt,
            contentColor = SelaPalette.onVolt,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Tombol sekunder — tonal redup. Untuk aksi pendukung.
 * Komponen #17 (Secondary Button).
 */
@Composable
fun SelaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = SelaTokens.pill,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Tombol garis tepi — untuk aksi netral seperti "Simpan URL".
 * Komponen #19 (Outline Button).
 */
@Composable
fun SelaOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
        shape = SelaTokens.pill,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Tombol teks — untuk aksi prioritas rendah seperti "Hapus riwayat".
 * Komponen #21 (Text Button).
 */
@Composable
fun SelaGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = SelaTokens.touchTarget),
        shape = SelaTokens.pill,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Tombol ikon melingkar di atas permukaan gelap — untuk dock peta.
 * Komponen #22 (Icon Button).
 */
@Composable
fun SelaRoundIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val background = if (accent) SelaPalette.volt else MaterialTheme.colorScheme.surfaceVariant
    val tint = if (accent) SelaPalette.onVolt else MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = background,
        border = if (accent) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
}
