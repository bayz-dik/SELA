package id.sela.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Statistik "gila" / fun-facts dari riwayat perjalanan (fitur #80).
 *
 * Semua angka di sini adalah estimasi kasar untuk hiburan — BUKAN data resmi.
 * Jarak tempuh diasumsikan 7,5 km per perjalanan (rata-rata rute TransJakarta
 * dalam kota, bukan pengukuran GPS per trip). UI wajib melabeli "estimasi kasar".
 */
data class WildStats(
    /** Total perjalanan tercatat. */
    val totalTrips: Int,
    /** Estimasi kasar jarak tempuh (km) = totalTrips × 7,5. */
    val estimatedKm: Double,
    /** Berapa kali jarak Jakarta–Bali (±1180 km) ditempuh, versi estimasi. */
    val jakartaBaliX: Double,
    /** Perjalanan yang dimulai antara 22:00–05:00. */
    val nightTrips: Int,
    /** Hari berurutan terpanjang dengan ≥1 perjalanan. */
    val longestStreakDays: Int,
    /** Timestamp perjalanan pertama, 0 bila belum ada. */
    val firstTripAt: Long,
)

/** Jarak Jakarta–Bali via darat/laut yang dipakai sebagai pembanding fun-fact. */
const val JAKARTA_BALI_KM = 1180.0

/** Asumsi jarak rata-rata satu perjalanan bus kota (km) — estimasi kasar. */
const val AVG_TRIP_KM = 7.5

/**
 * Hitung statistik gila dari [history]. Fungsi murni supaya bisa di-unit-test.
 * Entri bertimestamp di masa depan (jam HP kacau dsb.) diabaikan.
 */
fun computeWildStats(history: List<TripHistory>, nowMs: Long): WildStats {
    val sane = history.filter { it.timestamp <= nowMs }
    if (sane.isEmpty()) {
        return WildStats(0, 0.0, 0.0, 0, 0, 0L)
    }
    val tz = TimeZone.getTimeZone("Asia/Jakarta")
    fun dayKey(ts: Long): Long {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = ts }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    fun isNight(ts: Long): Boolean {
        val hour = Calendar.getInstance(tz).apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 5
    }

    val totalTrips = sane.size
    val estimatedKm = totalTrips * AVG_TRIP_KM
    val nightTrips = sane.count { isNight(it.timestamp) }

    val days = sane.map { dayKey(it.timestamp) }.toSortedSet()
    var longest = 0
    var current = 0
    var prev: Long? = null
    for (day in days) {
        current = if (prev != null && day - prev == 24L * 60 * 60 * 1000) current + 1 else 1
        longest = maxOf(longest, current)
        prev = day
    }

    return WildStats(
        totalTrips = totalTrips,
        estimatedKm = estimatedKm,
        jakartaBaliX = estimatedKm / JAKARTA_BALI_KM,
        nightTrips = nightTrips,
        longestStreakDays = longest,
        firstTripAt = sane.minOf { it.timestamp },
    )
}
