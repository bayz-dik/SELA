package id.sela.model

import kotlin.math.abs

/**
 * Kapsul waktu perjalanan (fitur #30): "setahun yang lalu kamu naik ...".
 *
 * Batasan jujur: "setahun" di sini adalah TETAP 365 hari (365 × 24 jam), BUKAN
 * tahun kalender — jadi pada tahun kabisat hasilnya meleset sekitar sehari dari
 * tanggal yang sama tahun lalu. Pencarian memakai toleransi ±2 jam supaya trip
 * yang waktunya mirip tetap ketemu walau tidak persis sama.
 */
object TimeCapsule {
    private const val DAYS_PER_YEAR = 365L
    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MILLIS_PER_HOUR = 3_600_000L

    /** Toleransi pencarian: ±2 jam dari titik tepat 365 hari lalu. */
    const val TOLERANCE_MS: Long = 2 * MILLIS_PER_HOUR

    /**
     * Mencari trip yang waktunya paling dekat dengan [nowMs] − 365 hari,
     * selama selisihnya tidak lebih dari [TOLERANCE_MS]. Bila beberapa trip
     * masuk toleransi, yang selisih waktunya paling kecil yang dikembalikan.
     *
     * @return trip terdekat, atau null bila tidak ada trip dalam toleransi.
     */
    fun aYearAgo(history: List<TripHistory>, nowMs: Long = System.currentTimeMillis()): TripHistory? {
        val target = nowMs - DAYS_PER_YEAR * MILLIS_PER_DAY
        return history
            .map { trip -> trip to abs(trip.timestamp - target) }
            .filter { (_, diff) -> diff <= TOLERANCE_MS }
            .minByOrNull { (_, diff) -> diff }
            ?.first
    }
}
