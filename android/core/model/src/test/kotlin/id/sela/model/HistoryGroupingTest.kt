package id.sela.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class HistoryGroupingTest {
    private val zone = ZoneId.of("Asia/Jakarta")

    private fun at(day: LocalDate, hour: Int): Long =
        day.atTime(LocalTime.of(hour, 0)).atZone(zone).toInstant().toEpochMilli()

    private fun entry(id: String, ts: Long) = TripHistory("r$id", "Rute $id", "s$id", "Halte $id", ts)

    @Test
    fun labelsTodayYesterdayAndDate() {
        val today = LocalDate.of(2026, 10, 14)
        val nowMs = at(today, 16)
        val history = listOf(
            entry("1", at(today, 9)),
            entry("2", at(today.minusDays(1), 8)),
            entry("3", at(today.minusDays(3), 7)),
        )
        val groups = groupHistoryByDay(history, nowMs, zone)
        assertEquals(listOf("Hari ini", "Kemarin", "11 Okt 2026"), groups.map { it.label })
    }

    @Test
    fun groupsAreNewestFirstAndEntriesSorted() {
        val today = LocalDate.of(2026, 10, 14)
        val nowMs = at(today, 16)
        val history = listOf(entry("2", at(today, 8)), entry("1", at(today, 9)))
        val groups = groupHistoryByDay(history, nowMs, zone)
        assertEquals(1, groups.size)
        assertEquals(listOf("r1", "r2"), groups.single().entries.map { it.routeId })
    }

    @Test
    fun emptyHistoryReturnsEmpty() {
        assertTrue(groupHistoryByDay(emptyList(), 0L, zone).isEmpty())
    }
}
