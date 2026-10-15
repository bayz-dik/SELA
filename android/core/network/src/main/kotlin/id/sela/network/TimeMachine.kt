package id.sela.network

import id.sela.database.SnapshotDao
import id.sela.database.VehicleSnapshotEntity
import id.sela.model.TransitRepository
import id.sela.model.Vehicle
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.asin
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Time machine (fitur #81): arsip posisi kendaraan + ekstrapolasi kasar.
 *
 * - [start]: setiap data kendaraan berubah, simpan snapshot (throttle 60 dtk,
 *   retensi 7 hari). B18: hanya kendaraan yang posisinya berubah > [MIN_MOVE_METERS]
 *   dari snapshot terakhirnya yang di-insert — bus ngetem/parkir tidak
 *   menulis baris berulang. Snapshot dipakai [atTime] dan [DisruptionMonitor].
 * - [atTime]: posisi bus pada waktu lampau — snapshot terdekat per kendaraan
 *   dalam ±5 menit dari [ts]. Kalau tidak ada snapshot, daftar kosong.
 * - [predictAhead]: "prediksi" posisi N menit ke depan dengan ekstrapolasi
 *   linear dari bearing + kecepatan terakhir. PREDIKSI KASAR: bus belok,
 *   berhenti di lampu merah, dan ngetem — garis lurus ini bukan ramalan.
 *
 * WIRING: daftarkan SnapshotDao di TransitDatabase + panggil start() saat app
 * dibuka (lihat pesan induk). Tanpa itu, start()/atTime() tidak aktif.
 */
@Singleton
class TimeMachine @Inject constructor(
    private val snapshotDao: SnapshotDao,
    private val repository: TransitRepository,
    @TransitScope private val scope: CoroutineScope,
) {
    companion object {
        const val SNAPSHOT_THROTTLE_MS = 60_000L
        const val RETENTION_MS = 7L * 24 * 60 * 60 * 1000
        const val AT_TIME_WINDOW_MS = 5 * 60 * 1000L

        /**
         * B18: kendaraan hanya ditulis ke DB kalau posisinya berubah lebih dari
         * ini (meter) dibanding snapshot terakhir yang tersimpan untuk
         * kendaraan itu. 10 m cukup untuk merekam pergerakan nyata tanpa
         * memasukkan noise jitter GPS ke dalam DB.
         */
        const val MIN_MOVE_METERS = 10.0

        /**
         * Ekstrapolasi linear posisi kendaraan [minutes] menit ke depan.
         * Fungsi murni — kendaraan tanpa data kecepatan dikembalikan apa adanya.
         */
        fun predictAhead(vehicles: List<Vehicle>, minutes: Int): List<Vehicle> {
            if (minutes <= 0) return vehicles
            return vehicles.map { v ->
                val speed = v.speedKmh?.takeIf { it > 0.5 } ?: return@map v
                val distanceKm = speed * minutes / 60.0
                val bearingRad = Math.toRadians(v.bearing)
                val latRad = Math.toRadians(v.lat)
                val newLat = v.lat + (distanceKm * cos(bearingRad)) / 111.32
                val newLon = v.lon + (distanceKm * Math.sin(bearingRad)) /
                    (111.32 * cos(latRad).coerceAtLeast(0.2))
                v.copy(lat = newLat, lon = newLon)
            }
        }
    }

    private var job: Job? = null
    private var lastSnapshotAt = 0L

    /** Posisi terakhir yang ditulis ke DB per vehicleId (lat, lon) — untuk filter B18. */
    private val lastSavedPositions = mutableMapOf<String, DoubleArray>()

    fun start() {
        if (job != null) return
        job = scope.launch {
            repository.data
                .map { it.vehicles }
                .distinctUntilChanged()
                .collect { vehicles ->
                    val now = System.currentTimeMillis()
                    if (vehicles.isEmpty() || now - lastSnapshotAt < SNAPSHOT_THROTTLE_MS) return@collect
                    lastSnapshotAt = now
                    // B18: insert hanya kendaraan yang bergerak signifikan sejak
                    // snapshot terakhirnya — cegah DB bloat dari armada ngetem.
                    val moved = vehicles.filter { v ->
                        val prev = lastSavedPositions[v.id]
                        prev == null || distanceMeters(prev[0], prev[1], v.lat, v.lon) > MIN_MOVE_METERS
                    }
                    runCatching {
                        if (moved.isNotEmpty()) {
                            snapshotDao.insertAll(
                                moved.map { v ->
                                    VehicleSnapshotEntity(
                                        vehicleId = v.id,
                                        routeId = v.routeId,
                                        lat = v.lat,
                                        lon = v.lon,
                                        timestamp = now,
                                    )
                                },
                            )
                            moved.forEach { v ->
                                lastSavedPositions[v.id] = doubleArrayOf(v.lat, v.lon)
                            }
                        }
                        snapshotDao.prune(now - RETENTION_MS)
                    }
                }
        }
    }

    /**
     * B18: jarak great-circle (haversine) dalam meter.
     * Fungsi murni — bisa di-unit-test.
     */
    internal fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    /**
     * Posisi kendaraan pada [ts] (epoch millis): snapshot terdekat per vehicleId
     * dalam ±[AT_TIME_WINDOW_MS]. Daftar kosong = tidak ada data saat itu.
     */
    suspend fun atTime(ts: Long): List<Vehicle> {
        val rows = try {
            snapshotDao.since(ts - AT_TIME_WINDOW_MS)
        } catch (_: Exception) {
            return emptyList()
        }
        return rows
            .filter { it.timestamp <= ts + AT_TIME_WINDOW_MS }
            .groupBy { it.vehicleId }
            .mapNotNull { (_, group) ->
                val nearest = group.minByOrNull { kotlin.math.abs(it.timestamp - ts) } ?: return@mapNotNull null
                Vehicle(
                    id = nearest.vehicleId,
                    routeId = nearest.routeId,
                    lat = nearest.lat,
                    lon = nearest.lon,
                    observedAt = Instant.ofEpochMilli(nearest.timestamp).toString(),
                    isStale = true, // data lampau — selalu tandai basi
                )
            }
    }
}
