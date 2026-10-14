package id.sela.power

import id.sela.a11y.LocalAnnouncer
import id.sela.a11y.packFor
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pengumum halte berikutnya / kedatangan via TTS dalam bahasa pilihan.
 *
 * BATASAN JUJUR (heuristik): aplikasi tidak tahu bus apa yang sedang dinaiki
 * pengguna. Fungsi [maybeAnnounce] hanya boleh dipanggil dari konteks yang jelas,
 * misalnya saat auto check-in aktif dan posisi HP dekat sebuah bus/halte.
 * Memanggilnya sembarangan akan menghasilkan pengumuman yang salah dan mengganggu.
 */
@Singleton
class NextStopAnnouncer @Inject constructor(
    private val announcer: LocalAnnouncer,
) {
    /**
     * Umumkan [stopName] kalau [announceEnabled] true.
     *
     * @param announceEnabled saklar dari pengaturan pengguna.
     * @param lang kode bahasa pack ("indonesia", "jawa", "sunda", "betawi";
     *   tidak dikenal → indonesia).
     * @param stopName nama halte yang diumumkan.
     * @param isArrived true = bus tiba di halte, false = halte berikutnya.
     */
    fun maybeAnnounce(
        announceEnabled: Boolean,
        lang: String,
        stopName: String,
        isArrived: Boolean,
    ) {
        if (!announceEnabled) return
        val pack = packFor(lang)
        val text = if (isArrived) pack.arrivedAnnouncement(stopName) else pack.nextStopAnnouncement(stopName)
        announcer.speak(text)
    }
}
