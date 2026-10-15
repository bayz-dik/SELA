package id.sela.a11y

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pembungkus TextToSpeech sistem Android yang sederhana.
 *
 * Batasan jujur:
 * - Memakai mesin TTS bawaan HP (Google TTS dsb.); kualitas & ketersediaan suara
 *   bahasa daerah mengikuti apa yang terinstal di HP pengguna, bukan dijamin app.
 * - Bahasa ucapan mengikuti Locale/data suara yang tersedia di perangkat;
 *   app tidak mengunduh paket suara sendiri.
 * - [speak] tidak bersuara apa-apa sebelum mesin TTS selesai init.
 */
@Singleton
class LocalAnnouncer @Inject constructor(
    @ApplicationContext context: Context,
) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
        }
    }

    /**
     * Ucapkan [text], memotong ucapan yang sedang berjalan (QUEUE_FLUSH).
     * Diam saja kalau TTS belum siap.
     */
    fun speak(text: String) {
        if (!ready) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sela-announce")
    }

    /** Coba set bahasa TTS; mengembalikan true kalau didukung perangkat. */
    fun trySetLanguage(locale: Locale): Boolean {
        val tts = tts ?: return false
        return when (tts.setLanguage(locale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> false
            else -> true
        }
    }

    /** Bebaskan mesin TTS. Panggil saat tidak dipakai lagi. */
    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
