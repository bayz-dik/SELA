package id.sela.power

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import id.sela.network.StandaloneTransitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Basis pendengar sensor satu-tipe dengan siklus start/stop. */
abstract class BaseSensorReader(context: Context) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var sensor: Sensor? = null

    fun start(sensorType: Int, delay: Int = SensorManager.SENSOR_DELAY_NORMAL) {
        if (sensor != null) return
        val found = manager.getDefaultSensor(sensorType) ?: return
        sensor = found
        manager.registerListener(this, found, delay)
    }

    fun stop() {
        manager.unregisterListener(this)
        sensor = null
    }

    protected fun currentSensor(): Sensor? = sensor

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/**
 * Pembaca barometer untuk menebak elevasi halte (fitur #100).
 *
 * Logikanya: simpan pembacaan tekanan pertama sebagai baseline, lalu bandingkan
 * pembacaan berikutnya. Tekanan turun > 0,35 hPa dari baseline → pengguna
 * kemungkinan berada di halte layang; naik > 0,35 hPa → di bawah/turunan.
 * Konversi kasar: ±0,12 hPa ≈ 1 meter.
 *
 * JUJUR: ini heuristik sangat kasar, BUKAN altimeter terkalibrasi. Perubahan
 * cuaca (mis. hujan mendekat) ikut menggeser tekanan udara dan bisa
 * mengacaukan tebakan. Angka meter hanya ordo perkiraan, jangan dipakai untuk
 * navigasi keselamatan.
 */
class BarometerReader(
    context: Context,
    private val onEstimate: (String) -> Unit,
) : BaseSensorReader(context) {
    private var baseline: Float? = null
    private var lastState: String? = null

    fun start() = start(Sensor.TYPE_PRESSURE)

    override fun onSensorChanged(event: SensorEvent?) {
        val pressure = event?.values?.getOrNull(0) ?: return
        val base = baseline
        if (base == null) {
            baseline = pressure
            return
        }
        val drop = base - pressure // positif = tekanan turun = posisi lebih tinggi
        val state = when {
            drop > 0.35f -> {
                val meters = (drop / 0.12f).roundToInt()
                "halte layang (±$meters m di atas jalan)"
            }
            drop < -0.35f -> "bawah/turunan"
            else -> "normal"
        }
        if (state != lastState && state != "normal") {
            lastState = state
            onEstimate(state)
        } else if (state == "normal") {
            lastState = null
        }
    }
}

/**
 * Penyarankan peta gelap dari sensor cahaya (fitur #101).
 *
 * Bila cahaya ambient di bawah 10 lux bertahan 3 detik, [onSuggestDark] dipanggil
 * sekali untuk sesi gelap itu (tidak di-spam setiap event sensor). Saat terang
 * kembali, status di-reset sehingga sesi gelap berikutnya bisa memicu lagi.
 */
class LightAdaptiveMap(
    context: Context,
    private val onSuggestDark: () -> Unit,
) : BaseSensorReader(context) {
    private var darkSinceMs: Long? = null
    private var suggested = false

    fun start() = start(Sensor.TYPE_LIGHT)

    override fun onSensorChanged(event: SensorEvent?) {
        val lux = event?.values?.getOrNull(0) ?: return
        val now = SystemClock.elapsedRealtime()
        if (lux < 10f) {
            val since = darkSinceMs ?: now.also { darkSinceMs = it }
            if (!suggested && now - since >= 3_000L) {
                suggested = true
                onSuggestDark()
            }
        } else {
            darkSinceMs = null
            suggested = false
        }
    }
}

/**
 * Bisukan notifikasi saat HP didekatkan ke telinga/saku (fitur #102).
 *
 * Memakai sensor proximity: nilai di bawah [Sensor.getMaximumRange] berarti ada
 * objek sangat dekat. Ada debounce 1 detik supaya satu gerakan tidak memicu
 * berkali-kali.
 */
class ProximityMute(
    context: Context,
    private val onMute: () -> Unit,
) : BaseSensorReader(context) {
    private var lastMuteMs = 0L

    fun start() = start(Sensor.TYPE_PROXIMITY)

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val maxRange = currentSensor()?.maximumRange ?: 5f
        if (event.values[0] < maxRange) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastMuteMs >= 1_000L) {
                lastMuteMs = now
                onMute()
            }
        }
    }
}

/**
 * Bisukan dengan membalik HP telungkup (fitur #103).
 *
 * Terdeteksi dari accelerometer sumbu-z < -8,5 (gravitasi ke arah layar) yang
 * bertahan 2 detik → [onFlip] dipanggil sekali, sampai HP diangkat lagi
 * (z > -7 me-reset status) sehingga tidak berulang.
 */
class FlipToMute(
    context: Context,
    private val onFlip: () -> Unit,
) : BaseSensorReader(context) {
    private var downSinceMs: Long? = null
    private var fired = false

    fun start() = start(Sensor.TYPE_ACCELEROMETER)

    override fun onSensorChanged(event: SensorEvent?) {
        val z = event?.values?.getOrNull(2) ?: return
        val now = SystemClock.elapsedRealtime()
        if (z < -8.5f) {
            val since = downSinceMs ?: now.also { downSinceMs = it }
            if (!fired && now - since >= 2_000L) {
                fired = true
                onFlip()
            }
        } else if (z > -7f) {
            downSinceMs = null
            fired = false
        }
    }
}

/**
 * Pendengar goyangan HP (fitur #98).
 *
 * G-force resultan accelerometer > 2,7g dengan debounce 1 detik → [onShake].
 * Ambang 2,7g dipilih supaya goyangan disengaja terdeteksi tapi guncangan
 * biasa di bus yang jalan tidak memicu terus-menerus. Bukan pengenal gestur
 * sempurna — gerakan keras lain tetap bisa terhitung sebagai goyangan.
 */
class ShakeListener(
    context: Context,
    private val onShake: () -> Unit,
) : BaseSensorReader(context) {
    private var lastShakeMs = 0L

    fun start() = start(Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_GAME)

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val x = event.values.getOrNull(0) ?: 0f
        val y = event.values.getOrNull(1) ?: 0f
        val z = event.values.getOrNull(2) ?: 0f
        val gForce = sqrt(x * x + y * y + z * z) / 9.81f
        if (gForce > 2.7f) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeMs >= 1_000L) {
                lastShakeMs = now
                onShake()
            }
        }
    }
}

/**
 * Goyang untuk memuat ulang data transit (fitur #98).
 *
 * Membungkus [ShakeListener]: setiap goyangan memicu [StandaloneTransitRepository.refresh]
 * di [scope] (dengan runCatching supaya kegagalan refresh diam-diam diabaikan,
 * tidak meledakkan pemanggil). Panggil [start]/[stop] mengikuti lifecycle layar.
 */
class ShakeToRefresh(
    context: Context,
    private val repository: StandaloneTransitRepository,
    private val scope: CoroutineScope,
) {
    private val listener = ShakeListener(context) {
        scope.launch {
            runCatching { repository.refresh() }
        }
    }

    fun start() = listener.start()
    fun stop() = listener.stop()
}
