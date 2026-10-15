package id.sela.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

/**
 * Test CrowdPredictor (fitur #17). Ambang: ≥8 RAMAI, ≥3 SEDANG, else SEPI.
 */
class CrowdPredictorTest {
    private val tz = TimeZone.getTimeZone("Asia/Jakarta")

    private fun tripAt(routeId: String, hour: Int, dayOffset: Int = 0): TripHistory {
        val cal = Calendar.getInstance(tz).apply {
            set(2026, Calendar.OCTOBER, 5 + dayOffset, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TripHistory(routeId, "Koridor $routeId", "S1", "Monas", cal.timeInMillis)
    }

    @Test fun emptyHistoryIsSepi() {
        assertEquals(CrowdLevel.SEPI, CrowdPredictor.predict("1", 7, emptyList()))
    }

    @Test fun fewTripsIsSepi() {
        val history = listOf(tripAt("1", 7), tripAt("1", 7, 1))
        assertEquals(CrowdLevel.SEPI, CrowdPredictor.predict("1", 7, history))
    }

    @Test fun threeTripsInBucketIsSedang() {
        val history = listOf(tripAt("1", 7), tripAt("1", 6, 1), tripAt("1", 7, 2))
        assertEquals(CrowdLevel.SEDANG, CrowdPredictor.predict("1", 7, history))
    }

    @Test fun eightTripsInBucketIsRamai() {
        val history = (0 until 8).map { tripAt("1", 17, it) }
        assertEquals(CrowdLevel.RAMAI, CrowdPredictor.predict("1", 17, history))
    }

    @Test fun ignoresOtherRoutesAndBuckets() {
        // 8 trip tapi beda rute / beda bucket → tetap SEPI untuk ("1", jam 7).
        val history = (0 until 8).map { tripAt("2", 7, it) } +
            (0 until 8).map { tripAt("1", 12, it) }
        assertEquals(CrowdLevel.SEPI, CrowdPredictor.predict("1", 7, history))
    }

    @Test fun hourBucketIsTwoHours() {
        assertEquals(3, CrowdPredictor.hourBucket(7)) // 6–8
        assertEquals(3, CrowdPredictor.hourBucket(6))
        assertEquals(11, CrowdPredictor.hourBucket(23))
    }
}
