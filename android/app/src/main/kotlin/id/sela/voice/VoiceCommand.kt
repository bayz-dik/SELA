package id.sela.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.SelaPastelShadows
import id.sela.designsystem.SelaPastelShapes
import id.sela.designsystem.pastelShadow

/**
 * Hasil parsing perintah suara (fitur #10 "Voice command").
 */
sealed interface VoiceAction {
    /** Tampilkan ETA untuk rute, mis. "1", "6B". */
    data class ShowEta(val routeQuery: String) : VoiceAction
    /** Tampilkan halte terdekat. */
    data object NearestStop : VoiceAction
    /** Arahkan pulang (butuh alamat rumah tersimpan — WIRING batch lain). */
    data object GoHome : VoiceAction
    /** Tidak dikenali. */
    data object Unknown : VoiceAction
}

/**
 * Parser perintah suara Bahasa Indonesia. Fungsi murni — bisa di-unit-test
 * tanpa Android.
 *
 * Contoh yang dikenali:
 * - "koridor 1 kapan datang" / "bus 1a kapan" / "kapan bus 6b datang" → ShowEta
 * - "halte terdekat" → NearestStop
 * - "antar aku pulang" / "pulang" → GoHome
 */
object VoiceCommand {
    private val routeAfterKeyword =
        Regex("""\b(koridor|bus|rute|jalur)\s*([0-9]{1,2}[a-zA-Z]?)""", RegexOption.IGNORE_CASE)
    private val routeBeforeKapan =
        Regex("""^([0-9]{1,2}[a-zA-Z]?)\s+(kapan|jam\s+berapa)""", RegexOption.IGNORE_CASE)

    fun parseCommand(text: String): VoiceAction {
        val t = text.trim().lowercase()
        if (t.isBlank()) return VoiceAction.Unknown
        if ("halte terdekat" in t || "halte paling dekat" in t || "halte dekat" in t) {
            return VoiceAction.NearestStop
        }
        if ("pulang" in t || "ke rumah" in t) {
            return VoiceAction.GoHome
        }
        val asksEta = "kapan" in t || "jam berapa" in t || "datang" in t
        if (asksEta) {
            routeAfterKeyword.find(t)?.let {
                return VoiceAction.ShowEta(it.groupValues[2].uppercase())
            }
            routeBeforeKapan.find(t)?.let {
                return VoiceAction.ShowEta(it.groupValues[1].uppercase())
            }
        }
        return VoiceAction.Unknown
    }
}

/**
 * Tombol mic: membuka dialog pengenalan suara Bahasa Indonesia, lalu
 * memanggil [onResult] dengan teks yang dikenali (atau tidak dipanggil bila
 * pengguna membatalkan / tidak ada hasil).
 *
 * Batasan jujur: memakai SpeechRecognizer bawaan Android — butuh koneksi
 * internet di kebanyakan perangkat (pengenalan suara on-device tidak
 * dijamin), dan akurasinya tergantung mesin Google di HP masing-masing.
 */
@Composable
fun VoiceInputButton(
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Tanya",
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!text.isNullOrBlank()) onResult(text)
        }
    }
    val available = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    // Pil kaca pastel — bukan Button gelap.
    Surface(
        onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Tanya SELA… mis. \"koridor 1 kapan datang\"")
            }
            runCatching { launcher.launch(intent) }
        },
        enabled = available,
        modifier = modifier.pastelShadow(
            elevation = SelaPastelShadows.soft,
            shape = SelaPastelShapes.pill,
        ),
        shape = SelaPastelShapes.pill,
        color = Color.White.copy(alpha = 0.88f),
        contentColor = SelaPastel.ink,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = SelaPastel.purpleDeep,
                modifier = Modifier.size(20.dp),
            )
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
