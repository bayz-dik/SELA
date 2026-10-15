package id.sela.a11y

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Paket frasa pengumuman suara (fitur #56 "suara kondektur").
 *
 * Dibuat sebagai interface supaya batch berikutnya bisa menambah paket
 * Bahasa Jawa / Sunda / Betawi tanpa mengubah pemanggil — cukup implementasi
 * [AnnouncementPack] baru + pilih di pengaturan.
 */
interface AnnouncementPack {
    /** Kode bahasa, mis. "indonesia", "jawa", "sunda", "betawi". */
    val lang: String
    /** Nama tampil untuk pengaturan, mis. "Bahasa Jawa". */
    val label: String
    /** Locale untuk mesin TTS, mis. Locale("id", "ID"). */
    val locale: Locale
    fun etaAnnouncement(stopName: String, etaMinutes: Int): String
    fun nextStopAnnouncement(stopName: String): String
    fun boardingReminder(routeName: String): String
    fun arrivedAnnouncement(stopName: String): String
}

/** Paket Bahasa Indonesia (default). */
object IndonesianPack : AnnouncementPack {
    override val lang: String = "indonesia"
    override val label: String = "Bahasa Indonesia"
    override val locale: Locale = Locale("id", "ID")

    override fun etaAnnouncement(stopName: String, etaMinutes: Int): String =
        if (etaMinutes <= 1) {
            "Bus menuju halte $stopName tiba kurang dari satu menit lagi. Siap-siap."
        } else {
            "Bus menuju halte $stopName tiba dalam $etaMinutes menit."
        }

    override fun nextStopAnnouncement(stopName: String): String =
        "Halte berikutnya: $stopName. Bagi yang turun, persiapkan diri."

    override fun boardingReminder(routeName: String): String =
        "Bus $routeName sudah dekat. Segera menuju pintu halte."

    override fun arrivedAnnouncement(stopName: String): String =
        "Kita telah tiba di halte $stopName."
}

/**
 * Wrapper TextToSpeech Bahasa Indonesia untuk SELA (fitur #66 "mode tunanetra").
 *
 * Batasan jujur: kualitas suara tergantung mesin TTS di HP (biasanya Google
 * TTS). Bila data suara Bahasa Indonesia belum diunduh di HP, sistem akan
 * memakai suara default / meminta unduh — SELA tidak membundel suara sendiri.
 */
class SelaTts(
    context: Context,
    private val pack: AnnouncementPack = IndonesianPack,
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(pack.locale)
                ready = result == TextToSpeech.LANG_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            }
        }
    }

    /** Bacakan ETA bus ke halte (dipakai mode tunanetra saat halte dipilih). */
    fun speakEta(stopName: String, etaMinutes: Int) {
        speak(pack.etaAnnouncement(stopName, etaMinutes))
    }

    /** Bacakan halte berikutnya (suara kondektur, fitur #56). */
    fun speakNextStop(stopName: String) {
        speak(pack.nextStopAnnouncement(stopName))
    }

    /** Pengingat naik bus saat bus favorit sudah dekat. */
    fun speakBoardingReminder(routeName: String) {
        speak(pack.boardingReminder(routeName))
    }

    private fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sela-${System.currentTimeMillis()}")
    }

    /** Panggil saat layar/activity selesai (hindari bocor). */
    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
