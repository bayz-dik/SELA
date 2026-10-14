package id.sela.network

import id.sela.model.EtaPredictor
import id.sela.model.Vehicle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keputusan murni auto check-in (fitur #63): apakah pengguna "kemungkinan"
 * sedang naik bus tertentu.
 *
 * Aturan: bila [autoCheckinEnabled] dan posisi pengguna berada dalam
 * [radiusM] dari sebuah bus yang bergerak (speedKmh > 5) selama
 * [dwellMs] berturut-turut, laporkan kandidat check-in.
 *
 * KDoc jujur: ini HEURISTIK. Berada dekat bus yang bergerak bukan bukti
 * pengguna naik bus itu (bisa jalan di trotoar sebelah bus). UI wajib
 * meminta konfirmasi ("Kamu naik bus koridor X?") sebelum recordTrip.
 */
object AutoCheckInDecider {
    const val DEFAULT_RADIUS_M = 60.0
    const val DEFAULT_DWELL_MS = 2 * 60 * 1000L

    /**
     * @param nearSinceMs timestamp pertama kali bus ini terdeteksi dekat
     *   (dikelola pemanggil/state tracker), atau null bila baru pertama kali.
     * @return true bila syarat waktu & jarak terpenuhi.
     */
    fun shouldCheckIn(
        userLat: Double,
        userLon: Double,
        vehicle: Vehicle,
        autoCheckinEnabled: Boolean,
        nowMs: Long,
        nearSinceMs: Long?,
        radiusM: Double = DEFAULT_RADIUS_M,
        dwellMs: Long = DEFAULT_DWELL_MS,
    ): Boolean {
        if (!autoCheckinEnabled) return false
        if (vehicle.isStale) return false
        if ((vehicle.speedKmh ?: 0.0) <= 5.0) return false
        val dist = EtaPredictor.haversineM(userLat, userLon, vehicle.lat, vehicle.lon)
        if (dist > radiusM) return false
        val since = nearSinceMs ?: return false
        return nowMs - since >= dwellMs
    }
}

/**
 * Tracker stateful auto check-in: mengingat sejak kapan tiap bus terlihat
 * dekat, lalu memanggil [StandaloneTransitRepository.recordTrip] saat
 * [AutoCheckInDecider] memutuskan. Satu instance per sesi realtime.
 *
 * Lokasi pengguna diterima sebagai parameter (modul ini tidak meminta
 * izin lokasi sendiri) — lihat WIRING untuk pemakaian di ForegroundLocation.
 */
class AutoCheckInTracker @Inject constructor(
    private val repository: StandaloneTransitRepository,
) {
    private val nearSince = mutableMapOf<String, Long>()

    /**
     * Evaluasi satu siklus lokasi. Mengembalikan vehicleId yang di-check-in,
     * atau null.
     */
    suspend fun evaluate(
        userLat: Double,
        userLon: Double,
        vehicles: List<Vehicle>,
        routeNameOf: (String) -> String?,
        autoCheckinEnabled: Boolean,
        nowMs: Long = System.currentTimeMillis(),
    ): String? {
        val seen = mutableSetOf<String>()
        for (v in vehicles) {
            if (v.isStale || (v.speedKmh ?: 0.0) <= 5.0) continue
            val dist = EtaPredictor.haversineM(userLat, userLon, v.lat, v.lon)
            if (dist > AutoCheckInDecider.DEFAULT_RADIUS_M) continue
            seen += v.id
            val since = nearSince.getOrPut(v.id) { nowMs }
            if (AutoCheckInDecider.shouldCheckIn(
                    userLat, userLon, v, autoCheckinEnabled, nowMs, since,
                )
            ) {
                nearSince.remove(v.id)
                runCatching {
                    repository.recordTrip(v.routeId, routeNameOf(v.routeId) ?: v.routeId, null, null)
                }
                return v.id
            }
        }
        nearSince.keys.retainAll(seen)
        return null
    }
}
