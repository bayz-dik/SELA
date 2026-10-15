package id.sela.power

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.SelaPastelShadows
import id.sela.designsystem.SelaPastelShapes
import id.sela.designsystem.pastelShadow
import id.sela.model.EtaPredictor
import id.sela.model.Stop
import id.sela.model.Vehicle
import kotlin.math.roundToInt

/**
 * Helper "Pulang" / go-home (fitur #53).
 *
 * [homeEtaText] memakai [EtaPredictor.estimate] terhadap halte rumah: hanya
 * kendaraan yang rutenya melayani halte itu yang dihitung, dan tetap berlaku
 * batasan estimasi lokal (jarak ≤ 15 km, ETA ≤ 60 menit). Halte rumah sendiri
 * diteruskan sebagai parameter ([homeStop]) — preferensi penyimpanannya belum
 * ada di [id.sela.network.TransitPreferences], jadi pemanggil yang
 * menyediakannya (mis. dari state layar).
 */
object GoHomeHelper {
    fun homeEtaText(homeStop: Stop?, vehicles: List<Vehicle>): String {
        if (homeStop == null) return "Belum ada halte rumah — atur dulu."
        val first = EtaPredictor.estimate(homeStop, vehicles, 1).firstOrNull()
        return if (first != null) {
            "Bus ke ${homeStop.name}: ±${first.etaMinutes.roundToInt()} mnt"
        } else {
            "Belum ada bus terpantau ke ${homeStop.name}."
        }
    }
}

/** Tombol aksi "Pulang" — pil kaca hangat dengan ikon rumah. */
@Composable
fun GoHomeFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.pastelShadow(
            elevation = SelaPastelShadows.card,
            shape = SelaPastelShapes.pill,
            color = SelaPastel.peachDeep,
        ),
        shape = SelaPastelShapes.pill,
        color = SelaPastel.sunny.copy(alpha = 0.92f),
        contentColor = SelaPastel.ink,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Pulang", style = MaterialTheme.typography.labelLarge)
        }
    }
}
