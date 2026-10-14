package id.sela.power

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaCard
import id.sela.designsystem.SelaSectionTitle

/**
 * Satu titik wisata terkurasi untuk mode turis.
 *
 * @param name Nama tempat.
 * @param desc Deskripsi satu kalimat.
 * @param nearbyStopHint Perkiraan halte TransJakarta terdekat.
 */
data class TouristSpot(
    val name: String,
    val desc: String,
    val nearbyStopHint: String,
)

/**
 * Kurasi tempat wisata Jakarta (fitur #58).
 *
 * JUJUR: daftar ini "pilihan editor" — 5 tempat populer yang dipilih manual,
 * data STATIS per 2026 (tidak diperbarui otomatis). Kolom halte terdekat
 * adalah PERKIRAAN kasar, bukan data resmi TransJakarta; halte bisa berubah
 * nama/rute. Jangan jadikan satu-satunya acuan navigasi.
 */
object TouristSpots {
    val SPOTS: List<TouristSpot> = listOf(
        TouristSpot(
            name = "Monas",
            desc = "Tugu ikon Jakarta dengan taman luas, cocok untuk jalan santai dan foto.",
            nearbyStopHint = "Halte Monas (perkiraan)",
        ),
        TouristSpot(
            name = "Kota Tua",
            desc = "Kawasan bersejarah dengan museum dan arsitektur kolonial Belanda.",
            nearbyStopHint = "Halte Kota (perkiraan)",
        ),
        TouristSpot(
            name = "Bundaran HI",
            desc = "Pusat kota dengan patung Selamat Datang dan pusat perbelanjaan.",
            nearbyStopHint = "Halte Bundaran HI (perkiraan)",
        ),
        TouristSpot(
            name = "Ancol",
            desc = "Kawasan rekreasi pantai: Dufan, SeaWorld, dan pantai pasir putih.",
            nearbyStopHint = "Halte Ancol (perkiraan)",
        ),
        TouristSpot(
            name = "TMII",
            desc = "Taman Mini Indonesia Indah: anjungan 33 provinsi dalam satu kawasan.",
            nearbyStopHint = "Halte TMII (perkiraan)",
        ),
    )
}

/** Kartu "Info wisata" berisi daftar [TouristSpots.SPOTS]. */
@Composable
fun TouristCard(modifier: Modifier = Modifier) {
    SelaCard(modifier = modifier) {
        SelaSectionTitle("Info wisata")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TouristSpots.SPOTS.forEachIndexed { index, spot ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(spot.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        spot.desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Halte terdekat: ${spot.nearbyStopHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (index < TouristSpots.SPOTS.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}
