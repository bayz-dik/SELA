package id.sela.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Tingkat kepadatan bus (fitur #17 "Prediksi kepadatan").
 */
enum class CrowdLevel {
    /** Sedikit penumpang — kemungkinan dapat tempat duduk. */
    SEPI,
    /** Cukup ramai — mungkin berdiri. */
    SEDANG,
    /** Penuh — jam sibuk. */
    RAMAI,
}

/**
 * Prediksi kepadatan bus dari riwayat perjalanan pribadi.
 *
 * Batasan jujur: ini PROKSI dari seberapa sering pengguna sendiri naik rute
 * tersebut di jam itu (dari tabel trip_history lokal), BUKAN data penumpang
 * real-time TransJakarta. Kalau pengguna jarang naik bus, prediksinya selalu
 * SEPI — dan itu benar untuk datanya, tapi bukan gambaran kondisi bus yang
 * sebenarnya. UI wajib melabelinya "perkiraan dari riwayatmu", bukan fakta.
 */
object CrowdPredictor {
    /** Bucket 2 jam: jam 7 → 3 (bucket 6–8). */
    fun hourBucket(hourOfDay: Int): Int = hourOfDay / 2

    const val THRESHOLD_RAMAI = 8
    const val THRESHOLD_SEDANG = 3

    /**
     * Prediksi kepadatan [routeId] pada [hourOfDay] (0–23) dari [history].
     * Menghitung kejadian (routeId, bucket 2 jam) di seluruh riwayat:
     * ≥8 → RAMAI, ≥3 → SEDANG, selainnya SEPI.
     */
    fun predict(routeId: String, hourOfDay: Int, history: List<TripHistory>): CrowdLevel {
        if (history.isEmpty()) return CrowdLevel.SEPI
        val bucket = hourBucket(hourOfDay.coerceIn(0, 23))
        val tz = TimeZone.getTimeZone("Asia/Jakarta")
        val count = history.count { trip ->
            if (trip.routeId != routeId) return@count false
            val cal = Calendar.getInstance(tz).apply { timeInMillis = trip.timestamp }
            hourBucket(cal.get(Calendar.HOUR_OF_DAY)) == bucket
        }
        return when {
            count >= THRESHOLD_RAMAI -> CrowdLevel.RAMAI
            count >= THRESHOLD_SEDANG -> CrowdLevel.SEDANG
            else -> CrowdLevel.SEPI
        }
    }
}
