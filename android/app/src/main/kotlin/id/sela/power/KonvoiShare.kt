package id.sela.power

import android.content.Intent

/**
 * Berbagi info keberangkatan ke teman/konvoi (fitur #71).
 *
 * Sepenuhnya tanpa server: teks dibuat lokal lalu dilempar ke share sheet
 * Android ([Intent.ACTION_SEND]) sehingga pengguna bisa mengirim via WhatsApp
 * atau aplikasi lain pilihannya. SELA tidak tahu siapa penerimanya dan tidak
 * menyimpan apa pun.
 */
object KonvoiShare {
    /**
     * Teks ajakan, mis.:
     * `Ikut bareng? Aku naik 1 dari Monas, ETA 5 mnt. (via SELA)`
     */
    fun shareText(routeName: String, stopName: String, etaMinutes: Int): String =
        "Ikut bareng? Aku naik $routeName dari $stopName, ETA $etaMinutes mnt. (via SELA)"

    /** Intent share teks polos untuk dibuka lewat chooser/share sheet. */
    fun shareIntent(text: String): Intent =
        Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
}
