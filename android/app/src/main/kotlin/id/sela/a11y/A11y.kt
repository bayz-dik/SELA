package id.sela.a11y

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import id.sela.network.SelaPrefs

/**
 * Mode aksesibilitas SELA (fitur #66 tunanetra, #121 kids mode, #122 elderly mode).
 *
 * - "lansia": skala font 1.35x — tombol & teks lebih besar.
 * - "anak": skala font 1.15x + bahasa sederhana (label UI tetap, disederhanakan
 *   bertahap oleh batch UI).
 * - "tunanetra": tidak mengubah tampilan; mengandalkan TalkBack bawaan Android
 *   + [SelaTts] membacakan ETA otomatis saat halte dipilih.
 *
 * Batasan jujur: [applyUiMode] memakai resources.updateConfiguration yang
 * deprecated sejak API 25 tapi masih berfungsi; efek penuh terlihat setelah
 * activity di-recreate. Untuk tunanetra, SELA tidak menggantikan TalkBack —
 * pastikan "Layanan aksesibilitas TalkBack" aktif di Pengaturan HP.
 */
object A11y {
    /**
     * Terapkan skala font sesuai [mode]. Kembalikan skala yang dipakai.
     */
    fun applyUiMode(activity: ComponentActivity, mode: String): Float {
        val scale = when (mode) {
            SelaPrefs.MODE_LANSIA -> 1.35f
            SelaPrefs.MODE_ANAK -> 1.15f
            else -> 1.0f // normal & tunanetra: skala sistem
        }
        val resources = activity.resources
        val config = Configuration(resources.configuration)
        if (config.fontScale != scale) {
            config.fontScale = scale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }
        return scale
    }
}

/**
 * Helper Compose: simpan satu instance [SelaTts] selama komposisi hidup dan
 * bacakan lewat [speak] setiap kali [triggerKey] berubah. Dipakai mis. saat
 * halte dipilih di peta pada mode "tunanetra".
 *
 * Contoh:
 * ```
 * TtsAnnouncer(selectedStop?.id) { tts ->
 *     val eta = ...
 *     tts.speakEta(selectedStop.name, eta)
 * }
 * ```
 */
@Composable
fun TtsAnnouncer(
    triggerKey: Any?,
    speak: (SelaTts) -> Unit,
) {
    val context = LocalContext.current
    val tts = remember { SelaTts(context) }
    DisposableEffect(triggerKey) {
        if (triggerKey != null) speak(tts)
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
}

/**
 * Umumkan ETA lewat TTS Bahasa Indonesia (fitur #66).
 * Panggil dari UI saat pengguna memilih halte dan mode = "tunanetra".
 */
fun announceEta(context: Context, stopName: String, etaMinutes: Int) {
    // TTS satu-kali: dibuat, dipakai, langsung dibuang setelah bicara.
    // (Untuk pemakaian intensif, simpan instance SelaTts di ViewModel.)
    val tts = SelaTts(context)
    tts.speakEta(stopName, etaMinutes)
    // Catatan: shutdown() TIDAK dipanggil di sini karena speak() asinkron;
    // biarkan GC + lifecycle process. Untuk pemakaian berulang, pakai TtsAnnouncer.
}
