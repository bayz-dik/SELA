package id.sela.power

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Detektor jatuh sederhana (fitur #124).
 *
 * Dua fase berurutan:
 * 1. Free-fall: magnitude accelerometer < 1,5 m/s² selama ≥ 300 ms
 *    (HP hampir tak merasakan gravitasi = sedang jatuh bebas).
 * 2. Impact: magnitude > 25 m/s² dalam 1 detik setelah fase 1
 *    (benturan keras saat mendarat).
 *
 * Jika keduanya terpenuhi → [onFall] dipanggil sekali, lalu detektor istirahat
 * (cooldown 5 detik) sebelum bisa memicu lagi.
 *
 * JUJUR: ini heuristik accelerometer, BUKAN alat pendeteksi jatuh medis.
 * False positive mungkin terjadi (mis. HP dilempar ke kasur, jatuh dari meja
 * rendah, atau gerakan olahraga ekstrem) dan false negative juga mungkin
 * (jatuh pelan ke permukaan empuk). Karena itu UI WAJIB menampilkan dialog
 * konfirmasi "Kamu oke?" dengan hitung mundur sebelum mengirim SOS beneran —
 * jangan pernah memicu panggilan/SOS otomatis langsung dari sini.
 */
class FallDetector(
    context: Context,
    private val onFall: () -> Unit,
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private enum class Phase { IDLE, FREE_FALL }

    private var phase = Phase.IDLE
    private var freeFallStartMs = 0L
    private var lastFireMs = 0L
    private var started = false

    fun start() {
        if (started) return
        started = true
        manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        started = false
        manager.unregisterListener(this)
        phase = Phase.IDLE
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastFireMs < 5_000L) return // cooldown 5 detik

        val x = event.values.getOrNull(0) ?: 0f
        val y = event.values.getOrNull(1) ?: 0f
        val z = event.values.getOrNull(2) ?: 0f
        val magnitude = sqrt(x * x + y * y + z * z)

        when (phase) {
            Phase.IDLE -> {
                if (magnitude < 1.5f) {
                    phase = Phase.FREE_FALL
                    freeFallStartMs = now
                }
            }
            Phase.FREE_FALL -> {
                val elapsed = now - freeFallStartMs
                when {
                    elapsed >= 300L && magnitude > 25f -> {
                        lastFireMs = now
                        phase = Phase.IDLE
                        onFall()
                    }
                    elapsed > 1_000L -> phase = Phase.IDLE // jatuh bebas tanpa benturan → reset
                }
            }
        }
    }
}
