package id.sela.model

import java.util.Calendar
import java.util.TimeZone

/**
 * Badge/lencana gamifikasi (fitur #18).
 *
 * @property id pengenal unik badge, dipakai sebagai kunci hasil [Gamification.earnedBadges].
 * @property name nama badge yang tampil ke pengguna.
 * @property desc penjelasan cara mendapatkan badge.
 */
data class Badge(
    val id: String,
    val name: String,
    val desc: String,
)

/**
 * Mesin badge gamifikasi SELA (fitur #18).
 *
 * Batasan jujur: semua badge dihitung murni dari riwayat perjalanan LOKAL
 * ([TripHistory]) yang tercatat di aplikasi ini. Ini BUKAN verifikasi bahwa
 * pengguna benar-benar naik bus — riwayat bisa tidak lengkap (dihapus atau
 * belum tercatat) atau tercatat ganda. Anggap badge sebagai penyemangat,
 * bukan sertifikat perjalanan yang terverifikasi.
 */
object Gamification {
    /** Semua badge yang tersedia. */
    val ALL: List<Badge> = listOf(
        Badge(
            id = "penjelajah-1",
            name = "Penjelajah",
            desc = "Selesaikan minimal 1 perjalanan.",
        ),
        Badge(
            id = "koridor-5",
            name = "Penakluk Koridor",
            desc = "Naik minimal 5 koridor/rute berbeda.",
        ),
        Badge(
            id = "komuter",
            name = "Komuter Sejati",
            desc = "Selesaikan minimal 10 perjalanan.",
        ),
        Badge(
            id = "burung-hantu",
            name = "Burung Hantu",
            desc = "Naik bus antara jam 22:00 dan 04:00 waktu lokal.",
        ),
        Badge(
            id = "setia-30",
            name = "Setia 30 Hari",
            desc = "Naik bus di 30 hari kalender berbeda yang berturut-turut.",
        ),
    )

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val STREAK_DAYS = 30

    /**
     * Menghitung id badge yang berhasil didapat dari [history].
     *
     * Kriteria:
     * - "penjelajah-1": riwayat berisi ≥1 trip.
     * - "koridor-5": ≥5 [TripHistory.routeId] unik.
     * - "komuter": ≥10 trip.
     * - "burung-hantu": ada trip yang jam lokalnya 22, 23, 0, 1, 2, 3, atau 4
     *   (zona waktu [TimeZone.getDefault]; granularitas per jam, jadi 04:59
     *   dihitung tapi 05:00 tidak).
     * - "setia-30": ada rangkaian 30 hari kalender berturut-turut yang
     *   masing-masing punya ≥1 trip. Hari kalender dihitung dari indeks hari
     *   zona lokal: floor((timestamp + offsetZona) / 86.400.000), dengan offset
     *   diambil per timestamp supaya mendekati hari kalender asli walau ada
     *   perubahan offset (mis. DST).
     *
     * @return himpunan id badge (kunci dari [Badge.id]); kosong bila [history] kosong.
     */
    fun earnedBadges(history: List<TripHistory>): Set<String> {
        if (history.isEmpty()) return emptySet()
        val earned = mutableSetOf<String>()
        earned += "penjelajah-1"
        if (history.size >= 10) earned += "komuter"
        if (history.map { it.routeId }.toSet().size >= 5) earned += "koridor-5"
        if (history.any { isNightOwl(it.timestamp) }) earned += "burung-hantu"
        if (hasThirtyDayStreak(history)) earned += "setia-30"
        return earned
    }

    /** True bila [timestampMs] jatuh pada jam lokal 22–04 (inklusif). */
    private fun isNightOwl(timestampMs: Long): Boolean {
        val hour = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = timestampMs
        }.get(Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour <= 4
    }

    /**
     * True bila ada rangkaian [STREAK_DAYS] hari kalender berturut-turut yang
     * masing-masing punya ≥1 trip. Beberapa trip di hari yang sama dihitung
     * sebagai satu hari.
     */
    private fun hasThirtyDayStreak(history: List<TripHistory>): Boolean {
        val zone = TimeZone.getDefault()
        val days = history.map { trip ->
            Math.floorDiv(trip.timestamp + zone.getOffset(trip.timestamp), MILLIS_PER_DAY)
        }.toSortedSet()
        var run = 1
        var previous = days.first()
        for (day in days.drop(1)) {
            run = if (day == previous + 1) run + 1 else 1
            if (run >= STREAK_DAYS) return true
            previous = day
        }
        return run >= STREAK_DAYS
    }
}
