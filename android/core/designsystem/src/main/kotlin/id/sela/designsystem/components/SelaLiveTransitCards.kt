package id.sela.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Kartu armada yang memakai status dari data kendaraan sungguhan.
 * Tidak menampilkan ETA, jadwal, atau jarak kecuali suatu saat ada sumber datanya.
 */
@Composable
fun SelaLiveBusCard(
    vehicleLabel: String,
    routeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SelaPastel.mintSoft,
    deep: Color = SelaPastel.mintDeep,
    speedKmh: Double? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = SelaPastelShapes.lg,
        border = BorderStroke(1.dp, SelaPastel.hairline),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = tint,
                shape = SelaPastelShapes.md,
                modifier = Modifier.size(52.dp),
            ) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = deep,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    vehicleLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SelaPastel.ink,
                )
                Text(
                    routeLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(shape = CircleShape, color = SelaPastel.mintSoft) {
                        Text(
                            "Posisi terkini",
                            color = SelaPastel.mintDeep,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                    speedKmh?.takeIf { it >= 0.0 && it.isFinite() }?.let {
                        Text(
                            "${it.toInt()} km/j",
                            color = SelaPastel.inkSoft,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = deep,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
