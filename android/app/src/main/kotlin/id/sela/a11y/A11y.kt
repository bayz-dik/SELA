package id.sela.a11y

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import id.sela.network.SelaPrefs
import java.util.Locale

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
 *
 * TTS satu-kali: engine dibuat, bicara dijadwalkan SETELAH init selesai
 * (init TTS itu asinkron — bicara langsung setelah konstruksi akan hilang),
 * lalu [TextToSpeech.shutdown] dipanggil saat ucapan selesai/gagal supaya
 * service connection tidak bocor. Ada pengaman shutdown paksa setelah
 * 60 detik seandainya listener tidak pernah terpanggil.
 * (Untuk pemakaian intensif/berulang, pakai [TtsAnnouncer] yang me-reuse
 * satu instance [SelaTts].)
 */
fun announceEta(context: Context, stopName: String, etaMinutes: Int) {
    val appCtx = context.applicationContext
    val text = IndonesianPack.etaAnnouncement(stopName, etaMinutes)
    val utteranceId = "sela-eta-${System.currentTimeMillis()}"
    var engine: TextToSpeech? = null
    val release: (TextToSpeech?) -> Unit = { t ->
        runCatching { t?.stop() }
        runCatching { t?.shutdown() }
    }
    engine = TextToSpeech(appCtx) { status ->
        val tts = engine
        if (status != TextToSpeech.SUCCESS || tts == null) {
            release(tts)
            return@TextToSpeech
        }
        val langResult = tts.setLanguage(Locale("id", "ID"))
        val langOk = langResult == TextToSpeech.LANG_AVAILABLE ||
            langResult == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            langResult == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        if (!langOk) {
            release(tts)
            return@TextToSpeech
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(doneId: String) {
                if (doneId == utteranceId) release(tts)
            }
            @Suppress("DEPRECATION", "OverridingDeprecatedMember")
            override fun onError(utteranceId: String) {
                release(tts)
            }
            override fun onError(utteranceId: String, errorCode: Int) {
                release(tts)
            }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        // Pengaman: paksa shutdown setelah 60 detik bila listener tak jalan.
        Handler(Looper.getMainLooper()).postDelayed({ release(tts) }, 60_000)
    }
}
