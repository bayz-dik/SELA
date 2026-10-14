package id.sela.model

import kotlin.math.*

/**
 * Satu estimasi kedatangan bus di sebuah halte (fitur #6 "Prediksi ETA lokal").
 *
 * Dihitung murni di perangkat dari posisi live terakhir kendaraan MQTT:
 * jarak haversine halte↔bus dibagi kecepatan bus (atau asumsi 18 km/jam bila
 * data kecepatan tidak ada). Ini ESTIMASI KASAR lokal — bukan jadwal resmi,
 * bukan klaim akurasi 99%. UI wajib melabelinya sebagai estimasi lokal.
 */
data class ArrivalEstimate(
    val routeId: String,
    val vehicleId: String,
    /** Jarak garis lurus halte↔bus dalam meter. */
    val distanceMeters: Double,
    /** Estimasi menit hingga tiba. */
    val etaMinutes: Double,
)

object EtaPredictor {
    /** Kecepatan asumsi bus kota bila payload MQTT tidak membawa kecepatan. */
    const val DEFAULT_SPEED_KMH = 18.0
    /** Estimasi hanya untuk bus dalam radius ini dari halte. */
    const val MAX_DISTANCE_M = 15_000.0
    /** Estimasi di atas ini dianggap tidak berguna dan dibuang. */
    const val MAX_ETA_MINUTES = 60.0

    /**
     * Estimasi kedatangan untuk [stop] dari daftar kendaraan live.
     * Hanya kendaraan yang rutenya melayani halte ini (routeId ∈ stop.routeIds)
     * dan tidak stale. Hasil diurutkan dari ETA tercepat, dibatasi [limit].
     */
    fun estimate(stop: Stop, vehicles: List<Vehicle>, limit: Int = 5): List<ArrivalEstimate> {
        if (stop.routeIds.isEmpty() || vehicles.isEmpty()) return emptyList()
        val served = stop.routeIds.toSet()
        return vehicles
            .asSequence()
            .filter { v -> !v.isStale && v.routeId in served }
            .mapNotNull { v ->
                val distM = haversineM(stop.lat, stop.lon, v.lat, v.lon)
                if (distM > MAX_DISTANCE_M) return@mapNotNull null
                val speedKmh = (v.speedKmh?.takeIf { it > 1.0 }) ?: DEFAULT_SPEED_KMH
                val etaMin = (distM / 1000.0) / speedKmh * 60.0
                if (etaMin > MAX_ETA_MINUTES) null
                else ArrivalEstimate(v.routeId, v.id, distM, etaMin)
            }
            .sortedBy { it.etaMinutes }
            .take(limit.coerceAtLeast(1))
            .toList()
    }

    fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 2 * r * asin(sqrt(a))
    }
}

/**
 * Ringkasan statistik riwayat perjalanan (fitur #2 "History + statistik").
 */
data class TripStats(
    val tripsThisWeek: Int,
    val topRouteName: String?,
    val topStopName: String?,
)

/**
 * Hitung statistik dari riwayat. Fungsi murni supaya bisa di-unit-test.
 * "Minggu ini" = 7×24 jam terakhir dari [nowMs].
 */
fun computeTripStats(history: List<TripHistory>, nowMs: Long): TripStats {
    val weekAgo = nowMs - 7L * 24 * 60 * 60 * 1000
    val recent = history.filter { it.timestamp >= weekAgo }
    val topRoute = history
        .filter { it.routeName.isNotBlank() }
        .groupingBy { it.routeName }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val topStop = history
        .filter { !it.stopName.isNullOrBlank() }
        .groupingBy { it.stopName!! }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    return TripStats(recent.size, topRoute, topStop)
}
