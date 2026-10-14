package id.sela.power

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.designsystem.SelaCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pengukur kebisingan sekitar (fitur #125).
 *
 * [start] mengembalikan [Flow] level suara dalam dB yang di-emit tiap 500 ms,
 * dihitung dari RMS sampel PCM_16BIT mikrofon: dB = 20·log10(rms/32768) + 90
 * (offset kalibrasi kasar supaya angkanya masuk akal di kisaran percakapan).
 *
 * WAJIB: izin runtime [Manifest.permission.RECORD_AUDIO]. Jika belum diizinkan,
 * flow langsung selesai kosong (tidak melempar). Panggil [stop] saat selesai
 * untuk melepas mikrofon — jangan biarkan AudioRecord jalan di background.
 *
 * JUJUR: ini ESTIMASI dari mikrofon HP, BUKAN sound level meter terkalibrasi.
 * Setiap model HP punya sensitivitas mikrofon berbeda; angka hanya berguna
 * sebagai perbandingan kasar (mis. "halte ini lebih bising dari tadi").
 */
class NoiseMeter @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: AudioRecord? = null
    private var recordingJob: Job? = null

    fun start(): Flow<Float> = callbackFlow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            close()
            return@callbackFlow
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL, ENCODING,
        )
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            close()
            return@callbackFlow
        }
        val bufferSize = minBuffer.coerceAtLeast(SAMPLE_RATE)
        val record = runCatching {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, bufferSize)
        }.getOrNull()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            close()
            return@callbackFlow
        }
        if (runCatching { record.startRecording() }.isFailure) {
            record.release()
            close()
            return@callbackFlow
        }

        recorder = record
        val buf = ShortArray(bufferSize)
        recordingJob = launch {
            while (isActive) {
                delay(500L)
                val read = runCatching { record.read(buf, 0, buf.size) }.getOrDefault(-1)
                if (read <= 0) continue
                var sum = 0.0
                for (i in 0 until read) {
                    val sample = buf[i] / 32768.0
                    sum += sample * sample
                }
                val rms = sqrt(sum / read).coerceAtLeast(1e-9)
                val db = (20.0 * log10(rms) + CALIBRATION_OFFSET_DB).toFloat()
                trySend(db)
            }
        }
        awaitClose { stop() }
    }

    /** Hentikan perekaman dan lepas mikrofon. Aman dipanggil berulang. */
    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        recorder?.let {
            runCatching { it.stop() }
            it.release()
        }
        recorder = null
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Offset kasar supaya angka mendekati dB(A) percakapan normal (~60-70 dB). */
        private const val CALIBRATION_OFFSET_DB = 90.0
    }
}

/**
 * Kartu tampilan kebisingan: "Kebisingan: 78 dB" + label kualitatif.
 *
 * - < 55 dB → "Tenang"
 * - < 75 dB → "Sedang"
 * - ≥ 75 dB → "Bising"
 */
@Composable
fun NoiseCard(db: Float, modifier: Modifier = Modifier) {
    val label = when {
        db < 55f -> "Tenang"
        db < 75f -> "Sedang"
        else -> "Bising"
    }
    SelaCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Kebisingan", style = MaterialTheme.typography.titleMedium)
            Text(
                "${db.roundToInt()} dB",
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
