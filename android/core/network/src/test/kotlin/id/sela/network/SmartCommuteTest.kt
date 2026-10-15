package id.sela.network

import id.sela.database.TransitDao
import id.sela.database.TripHistoryEntity
import io.mockk.coEvery
import io.mockk.mockk
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Test SmartCommute (fitur #8) dengan DAO palsu.
 */
class SmartCommuteTest {
    private val tz = TimeZone.getTimeZone("Asia/Jakarta")
    private val now = Calendar.getInstance(tz).apply {
        // Rabu 2026-10-14 12:00 WIB.
        set(2026, Calendar.OCTOBER, 14, 12, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun entry(day: Int, hour: Int, routeId: String = "1"): TripHistoryEntity {
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, day, hour, 0, 0); set(Calendar.MILLISECOND, 0)
        }
        return TripHistoryEntity(
            routeId = routeId, routeName = "Koridor $routeId",
            stopId = "S1", stopName = "Monas", timestamp = cal.timeInMillis,
        )
    }

    private fun commute(dao: TransitDao) = SmartCommute(dao)

    @Test fun emptyHistoryYieldsNoPatterns() = runTest {
        val dao = mockk<TransitDao> { coEvery { tripHistory(any()) } returns emptyList() }
        assertTrue(commute(dao).learn().isEmpty())
    }

    @Test fun learnsPagiPatternAfterThreeOccurrences() = runTest {
        // Senin 12, Selasa 13, Rabu 14 Okt 2026 jam 7 → weekday + pagi, 3x.
        val rows = listOf(entry(12, 7), entry(13, 7), entry(14, 7))
        val dao = mockk<TransitDao> { coEvery { tripHistory(any()) } returns rows }
        val patterns = commute(dao).learn()
        assertEquals(1, patterns.size)
        val p = patterns.single()
        assertEquals("1", p.routeId)
        assertEquals("weekday", p.dayType)
        assertEquals("pagi", p.hourBucket)
        assertEquals(3, p.count)
    }

    @Test fun ignoresOutsideCommuteHours() = runTest {
        // Jam 12 siang bukan jam komuter → tidak jadi pola.
        val rows = listOf(entry(12, 12), entry(13, 12), entry(14, 12))
        val dao = mockk<TransitDao> { coEvery { tripHistory(any()) } returns rows }
        assertTrue(commute(dao).learn().isEmpty())
    }

    @Test fun oldEntriesBeyondSixtyDaysIgnored() = runTest {
        val old = entry(1, 7).copy(timestamp = now - 61L * 24 * 60 * 60 * 1000)
        val rows = listOf(entry(12, 7), entry(13, 7), entry(14, 7), old)
        val dao = mockk<TransitDao> { coEvery { tripHistory(any()) } returns rows }
        val patterns = commute(dao).learn()
        assertEquals(3, patterns.single().count) // entri lama tidak dihitung
    }

    @Test fun suggestionForMatchesBucketAboutAnHourBefore() {
        val smart = commute(mockk())
        val patterns = listOf(
            CommutePattern("1", "Koridor 1", "S1", "Monas", "weekday", "pagi", 5),
            CommutePattern("2", "Koridor 2", "S2", "Harmoni", "weekday", "sore", 4),
        )
        fun at(day: Int, hour: Int) = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, day, hour, 0, 0)
        }
        // Rabu jam 6 pagi → saran pola pagi.
        val pagi = smart.suggestionFor(at(14, 6), patterns)
        assertEquals("1", pagi?.routeId)
        // Rabu jam 16 → saran pola sore.
        val sore = smart.suggestionFor(at(14, 16), patterns)
        assertEquals("2", sore?.routeId)
        // Rabu jam 12 → tidak ada saran.
        assertNull(smart.suggestionFor(at(14, 12), patterns))
        // Sabtu jam 6 → tidak ada saran (pola hanya weekday).
        assertNull(smart.suggestionFor(at(10, 6), patterns))
    }
}
