package id.sela.model

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

/** Uji [TimeCapsule.aYearAgo]: pencarian trip ~365 hari lalu termasuk toleransi. */
class TimeCapsuleTest {

    private val nowMs = Instant.parse("2026-10-14T12:00:00Z").toEpochMilli()
    private val yearMs = 365L * 86_400_000L
    private val hourMs = 3_600_000L
    private val target = nowMs - yearMs

    private fun trip(timestamp: Long, routeId: String = "1") =
        TripHistory(routeId, "Koridor $routeId", null, null, timestamp)

    @Test fun riwayatKosongMengembalikanNull() {
        assertNull(TimeCapsule.aYearAgo(emptyList(), nowMs))
    }

    @Test fun tripTepatSetahunLaluDitemukan() {
        val expected = trip(target)
        assertEquals(expected, TimeCapsule.aYearAgo(listOf(expected), nowMs))
    }

    @Test fun tripDiLuarToleransiDihiraukan() {
        val tooEarly = trip(target - 2 * hourMs - 1)
        val tooLate = trip(target + 2 * hourMs + 1)
        assertNull(TimeCapsule.aYearAgo(listOf(tooEarly, tooLate), nowMs))
    }

    @Test fun tripTepatDiBatasToleransiDiterima() {
        val early = trip(target - 2 * hourMs)
        val late = trip(target + 2 * hourMs)
        assertEquals(early, TimeCapsule.aYearAgo(listOf(early), nowMs))
        assertEquals(late, TimeCapsule.aYearAgo(listOf(late), nowMs))
    }

    @Test fun tripDenganSelisihTerkecilYangDipilih() {
        val near = trip(target + 30 * 60_000L, "dekat")
        val far = trip(target - 90 * 60_000L, "jauh")
        assertEquals(near, TimeCapsule.aYearAgo(listOf(far, near), nowMs))
    }

    @Test fun tigaRatusEnamPuluhEnamHariDiLuarToleransi() {
        // Titik acuan tetap 365 hari, jadi 366 hari lalu berada di luar ±2 jam.
        assertNull(TimeCapsule.aYearAgo(listOf(trip(nowMs - 366L * 86_400_000L)), nowMs))
    }
}
