package id.sela.power

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/** Tombol aksi "Pulang" — ExtendedFloatingActionButton ikon rumah. */
@Composable
fun GoHomeFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        icon = { Icon(Icons.Filled.Home, contentDescription = "Pulang") },
        text = { Text("Pulang") },
    )
}
