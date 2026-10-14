package id.sela.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Uji [Gamification]: kriteria tiap badge termasuk kasus batas dan riwayat kosong. */
class GamificationTest {

    private val jakarta: TimeZone = TimeZone.getTimeZone("Asia/Jakarta")
    private var savedDefault: TimeZone? = null

    @Before
    fun pakaiZonaJakarta() {
        savedDefault = TimeZone.getDefault()
        TimeZone.setDefault(jakarta)
    }

    @After
    fun kembalikanZonaDefault() {
        TimeZone.setDefault(savedDefault)
    }

    /**
     * Trip pada 1 September 2026 jam [hour]:[minute] waktu Jakarta,
     * digeser [dayOffset] hari ke depan.
     */
    private fun tripPada(
        dayOffset: Int = 0,
        hour: Int = 12,
        minute: Int = 0,
        routeId: String = "1",
    ): TripHistory {
        val cal = Calendar.getInstance(jakarta).apply {
            set(2026, Calendar.SEPTEMBER, 1, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TripHistory(routeId, "Koridor $routeId", "S1", "Halte A", cal.timeInMillis + dayOffset * 86_400_000L)
    }

    @Test fun riwayatKosongTidakDapatBadge() {
        assertTrue(Gamification.earnedBadges(emptyList()).isEmpty())
    }

    @Test fun satuTripDapatPenjelajah() {
        assertEquals(setOf("penjelajah-1"), Gamification.earnedBadges(listOf(tripPada())))
    }

    @Test fun sembilanTripBelumDapatKomuter() {
        val history = List(9) { tripPada(dayOffset = it) }
        assertFalse("komuter" in Gamification.earnedBadges(history))
    }

    @Test fun sepuluhTripDapatKomuter() {
        val history = List(10) { tripPada(dayOffset = it) }
        val earned = Gamification.earnedBadges(history)
        assertTrue("penjelajah-1" in earned)
        assertTrue("komuter" in earned)
    }

    @Test fun limaKoridorUnikDapatKoridor5() {
        val history = (1..5).map { tripPada(routeId = "$it") }
        assertTrue("koridor-5" in Gamification.earnedBadges(history))
    }

    @Test fun empatKoridorBelumDapatKoridor5() {
        val history = (1..4).map { tripPada(routeId = "$it") }
        assertFalse("koridor-5" in Gamification.earnedBadges(history))
    }

    @Test fun tripTengahMalamDapatBurungHantu() {
        assertTrue("burung-hantu" in Gamification.earnedBadges(listOf(tripPada(hour = 23, minute = 30))))
    }

    @Test fun tripSiangTidakDapatBurungHantu() {
        assertFalse("burung-hantu" in Gamification.earnedBadges(listOf(tripPada(hour = 12))))
    }

    @Test fun batasJamBurungHantu() {
        assertTrue("burung-hantu" in Gamification.earnedBadges(listOf(tripPada(hour = 22, minute = 0))))
        assertTrue("burung-hantu" in Gamification.earnedBadges(listOf(tripPada(hour = 4, minute = 59))))
        assertFalse("burung-hantu" in Gamification.earnedBadges(listOf(tripPada(hour = 21, minute = 59))))
        assertFalse("burung-hantu" in Gamification.earnedBadges(listOf(tripPada(hour = 5, minute = 0))))
    }

    @Test fun tigaPuluhHariBerkesinambunganDapatSetia30() {
        val history = (0 until 30).map { tripPada(dayOffset = it) }
        assertTrue("setia-30" in Gamification.earnedBadges(history))
    }

    @Test fun duaPuluhSembilanHariBelumCukup() {
        val history = (0 until 29).map { tripPada(dayOffset = it) }
        assertFalse("setia-30" in Gamification.earnedBadges(history))
    }

    @Test fun jedaSehariMemutusStreak() {
        val history = (0 until 20).map { tripPada(dayOffset = it) } +
            (21 until 41).map { tripPada(dayOffset = it) }
        assertFalse("setia-30" in Gamification.earnedBadges(history))
    }

    @Test fun banyakTripDiHariSamaTidakMenambahStreak() {
        val history = List(30) { tripPada(dayOffset = 0) }
        val earned = Gamification.earnedBadges(history)
        assertFalse("setia-30" in earned)
        assertTrue("komuter" in earned)
    }

    @Test fun daftarBadgeLengkap() {
        assertEquals(
            listOf("penjelajah-1", "koridor-5", "komuter", "burung-hantu", "setia-30"),
            Gamification.ALL.map { it.id },
        )
    }
}
