package id.sela.feature.map

import android.content.Context
import android.content.Intent
import kotlin.math.roundToInt

/**
 * Bagikan ETA bus ke aplikasi lain (fitur #12 "Share ETA").
 *
 * Mengirim teks polos via chooser sistem — tanpa akun, tanpa server.
 * Teks memakai format: "Aku naik [rute] dari halte [halte] — estimasi
 * tiba [X] mnt lagi. (via SELA)".
 */
fun shareEta(context: Context, stopName: String, routeName: String, etaMinutes: Double) {
    val text = "Aku naik $routeName dari halte $stopName — " +
        "estimasi tiba ${etaMinutes.roundToInt()} mnt lagi. (via SELA)"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan ETA"))
}
