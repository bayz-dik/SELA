package id.sela.network

import id.sela.database.SnapshotDao
import id.sela.database.VehicleSnapshotEntity
import id.sela.model.TransitRepository
import id.sela.model.Vehicle
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Time machine (fitur #81): arsip posisi kendaraan + ekstrapolasi kasar.
 *
 * - [start]: setiap data kendaraan berubah, simpan snapshot (throttle 60 dtk,
 *   retensi 7 hari). Snapshot dipakai [atTime] dan [DisruptionMonitor].
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
                    runCatching {
                        snapshotDao.insertAll(
                            vehicles.map { v ->
                                VehicleSnapshotEntity(
                                    vehicleId = v.id,
                                    routeId = v.routeId,
                                    lat = v.lat,
                                    lon = v.lon,
                                    timestamp = now,
                                )
                            },
                        )
                        snapshotDao.prune(now - RETENTION_MS)
                    }
                }
        }
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
