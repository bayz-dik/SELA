package id.sela.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

/**
 * Test WildStats (fitur #80). Semua angka estimasi kasar — test memverifikasi
 * rumus, bukan kebenaran jarak sebenarnya.
 */
class WildStatsTest {
    private val tz = TimeZone.getTimeZone("Asia/Jakarta")
    private val now = Calendar.getInstance(tz).apply {
        set(2026, Calendar.OCTOBER, 14, 12, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun trip(day: Int, hour: Int): TripHistory {
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, day, hour, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        return TripHistory("1", "Koridor 1", "S1", "Monas", cal.timeInMillis)
    }

    @Test fun emptyHistory() {
        val stats = computeWildStats(emptyList(), now)
        assertEquals(0, stats.totalTrips)
        assertEquals(0.0, stats.estimatedKm, 0.0)
        assertEquals(0, stats.longestStreakDays)
        assertEquals(0L, stats.firstTripAt)
    }

    @Test fun estimatedKmIsTripsTimes7_5() {
        val stats = computeWildStats(listOf(trip(10, 7), trip(11, 7)), now)
        assertEquals(2, stats.totalTrips)
        assertEquals(15.0, stats.estimatedKm, 1e-9)
        assertEquals(15.0 / 1180.0, stats.jakartaBaliX, 1e-9)
    }

    @Test fun nightTripsCount22to5() {
        val history = listOf(trip(10, 23), trip(10, 3), trip(10, 7), trip(10, 21))
        val stats = computeWildStats(history, now)
        assertEquals(2, stats.nightTrips) // jam 23 & 3; jam 21 & 7 bukan malam
    }

    @Test fun longestStreakCountsConsecutiveDays() {
        // 10, 11, 12 berurutan (3 hari), 14 sendiri → streak terpanjang 3.
        val history = listOf(trip(10, 7), trip(10, 18), trip(11, 7), trip(12, 7), trip(14, 7))
        val stats = computeWildStats(history, now)
        assertEquals(3, stats.longestStreakDays)
    }

    @Test fun firstTripAtIsEarliest() {
        val history = listOf(trip(12, 7), trip(10, 7), trip(11, 7))
        val stats = computeWildStats(history, now)
        assertEquals(trip(10, 7).timestamp, stats.firstTripAt)
    }

    @Test fun futureDatedEntriesIgnored() {
        val future = TripHistory("1", "Koridor 1", "S1", "Monas", now + 99_999_999L)
        val stats = computeWildStats(listOf(trip(10, 7), future), now)
        assertEquals(1, stats.totalTrips)
    }
}
