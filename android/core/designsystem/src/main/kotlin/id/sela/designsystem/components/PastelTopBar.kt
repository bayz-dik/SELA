package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Bilah atas pastel — tombol kembali lingkaran kaca + judul tinta.
 * Untuk layar bertumpuk (Riwayat, Rencana, Peta offline, dll).
 */
@Composable
fun PastelTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            modifier = Modifier
                .size(48.dp)
                .pastelShadow(elevation = SelaPastelShadows.soft, shape = CircleShape),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = SelaPastel.ink)
            }
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = SelaPastel.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}
