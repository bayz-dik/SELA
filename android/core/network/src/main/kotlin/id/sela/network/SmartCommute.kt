package id.sela.network

import id.sela.database.TransitDao
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Satu pola perjalanan yang dipelajari dari riwayat (fitur #8 "Smart commute").
 */
data class CommutePattern(
    val routeId: String,
    val routeName: String,
    val stopId: String?,
    val stopName: String?,
    /** "weekday" (Senin–Jumat) atau "weekend" (Sabtu–Minggu). */
    val dayType: String,
    /** "pagi" (05–09) atau "sore" (16–20). */
    val hourBucket: String,
    val count: Int,
)

/**
 * Smart commute: mempelajari pola perjalanan dari riwayat lokal 60 hari.
 *
 * Batasan jujur: pola dipelajari dari riwayat yang TERCATAT di aplikasi ini
 * saja (pengguna harus menekan "catat perjalanan" / perjalanan tercatat
 * otomatis oleh fitur #2). Bukan prediksi AI generik — murni hitung frekuensi
 * grup (rute, halte, tipe hari, slot jam) dengan ≥3 kejadian.
 */
@Singleton
class SmartCommute @Inject constructor(
    private val dao: TransitDao,
) {
    companion object {
        private val JAKARTA = TimeZone.getTimeZone("Asia/Jakarta")
        const val HISTORY_DAYS = 60
        const val MIN_OCCURRENCES = 3
    }

    /**
     * Pelajari pola dari trip_history 60 hari terakhir.
     * Murni agregasi di memori; cocok untuk riwayat ribuan entri.
     */
    suspend fun learn(): List<CommutePattern> {
        val cutoff = System.currentTimeMillis() - HISTORY_DAYS * 24L * 60 * 60 * 1000
        val rows = dao.tripHistory(limit = 5000).filter { it.timestamp >= cutoff }
        if (rows.isEmpty()) return emptyList()

        data class Key(
            val routeId: String,
            val routeName: String,
            val stopId: String?,
            val stopName: String?,
            val dayType: String,
            val hourBucket: String,
        )

        val counts = mutableMapOf<Key, Int>()
        for (row in rows) {
            val cal = Calendar.getInstance(JAKARTA).apply { timeInMillis = row.timestamp }
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val dayType = if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) "weekend" else "weekday"
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val bucket = when (hour) {
                in 5..9 -> "pagi"
                in 16..20 -> "sore"
                else -> continue // di luar jam komuter, abaikan
            }
            val key = Key(row.routeId, row.routeName, row.stopId, row.stopName, dayType, bucket)
            counts[key] = (counts[key] ?: 0) + 1
        }
        return counts
            .filter { it.value >= MIN_OCCURRENCES }
            .map { (key, count) ->
                CommutePattern(
                    routeId = key.routeId,
                    routeName = key.routeName,
                    stopId = key.stopId,
                    stopName = key.stopName,
                    dayType = key.dayType,
                    hourBucket = key.hourBucket,
                    count = count,
                )
            }
            .sortedByDescending { it.count }
    }

    /**
     * Saran pola untuk [now]: pola yang slot jamnya dimulai ~1 jam dari sekarang
     * (waktu siap-siap berangkat) dan tipe harinya cocok.
     * "pagi" disarankan saat jam 4–8, "sore" saat jam 15–19.
     */
    fun suggestionFor(now: Calendar, patterns: List<CommutePattern>): CommutePattern? {
        if (patterns.isEmpty()) return null
        val dow = now.get(Calendar.DAY_OF_WEEK)
        val dayType = if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) "weekend" else "weekday"
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return patterns
            .filter { it.dayType == dayType }
            .filter { pattern ->
                when (pattern.hourBucket) {
                    "pagi" -> hour in 4..8
                    "sore" -> hour in 15..19
                    else -> false
                }
            }
            .maxByOrNull { it.count }
    }
}
