package id.sela.sos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detektor guncangan untuk SOS (fitur #123 "SOS goyang 3x").
 *
 * Cara kerja: percepatan total (dikurangi gravitasi) yang melewati ambang
 * [SHAKE_THRESHOLD_G] dihitung sebagai 1 guncangan; bila [REQUIRED_SHAKES]
 * guncangan terjadi dalam [WINDOW_MS], [onShake] dipanggil.
 *
 * Batasan jujur: deteksi accelerometer itu kasar — jalan berlubang atau HP
 * jatuh dari meja bisa memicu false positive. Karena itu SOS goyang TIDAK
 * langsung mengirim apa-apa: ia hanya membuka dialog konfirmasi SOS (yang bisa
 * dibatalkan dalam 5 detik).
 */
class ShakeDetector(
    private val onShake: () -> Unit,
) : SensorEventListener {
    companion object {
        /** Ambang guncangan dalam G (di luar gravitasi 1G). */
        const val SHAKE_THRESHOLD_G = 2.2f
        const val REQUIRED_SHAKES = 3
        const val WINDOW_MS = 4000L
        /** Jeda antar guncangan supaya satu guncangan panjang tidak dihitung ganda. */
        const val MIN_INTERVAL_MS = 400L
    }

    private var sensorManager: SensorManager? = null
    private val shakeTimes = ArrayDeque<Long>()
    private var lastShakeAt = 0L

    fun start(context: Context) {
        if (sensorManager != null) return
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensorManager = manager
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        shakeTimes.clear()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH
        val gForce = kotlin.math.sqrt(x * x + y * y + z * z) - 1f
        if (gForce < SHAKE_THRESHOLD_G) return

        val now = System.currentTimeMillis()
        if (now - lastShakeAt < MIN_INTERVAL_MS) return
        lastShakeAt = now

        shakeTimes.addLast(now)
        while (shakeTimes.isNotEmpty() && now - shakeTimes.first() > WINDOW_MS) {
            shakeTimes.removeFirst()
        }
        if (shakeTimes.size >= REQUIRED_SHAKES) {
            shakeTimes.clear()
            onShake()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
